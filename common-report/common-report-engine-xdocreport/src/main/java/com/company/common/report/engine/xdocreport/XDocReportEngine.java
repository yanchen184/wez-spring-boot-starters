package com.company.common.report.engine.xdocreport;

import com.company.common.report.enums.OutputFormat;
import com.company.common.report.enums.ReportEngineType;
import com.company.common.report.spi.ImageSource;
import com.company.common.report.spi.ReportContext;
import com.company.common.report.spi.ReportEngine;
import com.company.common.report.spi.ReportResult;
import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.images.ByteArrayImageProvider;
import fr.opensagres.xdocreport.document.images.IImageProvider;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * xDocReport 報表引擎實作
 *
 * <p>使用 DOCX 範本搭配 Velocity 模板引擎產製文件。
 * <ul>
 *   <li>DOCX 輸出：直接 process（不轉換）</li>
 *   <li>PDF 輸出：透過 xDocReport converter 轉換（需引入 converter 依賴）</li>
 *   <li>圖片插入：透過 {@link ReportContext#getImages()} 註冊圖片欄位</li>
 * </ul>
 */
public class XDocReportEngine implements ReportEngine {

    private static final Logger log = LoggerFactory.getLogger(XDocReportEngine.class);

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private static final Set<OutputFormat> SUPPORTED_FORMATS = Set.of(
            OutputFormat.DOCX, OutputFormat.PDF, OutputFormat.ODT
    );

    @Override
    public ReportEngineType getType() {
        return ReportEngineType.XDOCREPORT;
    }

    @Override
    public boolean supports(OutputFormat format) {
        return SUPPORTED_FORMATS.contains(format);
    }

    @Override
    public ReportResult generate(ReportContext context) {
        log.info("--> xDocReport generate | format={}, template={}, images={}",
                context.getOutputFormat(), context.getTemplatePath(),
                context.getImages() != null ? context.getImages().size() : 0);

        if (context.getTemplatePath() == null || context.getTemplatePath().isBlank()) {
            throw new IllegalArgumentException(
                    "templatePath is required for xDocReport engine");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InputStream templateStream = loadTemplate(context.getTemplatePath())) {
            // 1. 載入範本
            IXDocReport report = XDocReportRegistry.getRegistry()
                    .loadReport(templateStream, TemplateEngineKind.Velocity);

            // 2. 註冊 metadata（圖片 + list 欄位）
            registerImageMetadata(report, context.getImages());
            registerListMetadata(report, context.getData());

            // 3. 建立 context 並填入參數
            IContext velocityContext = report.createContext();
            if (context.getParameters() != null) {
                context.getParameters().forEach(velocityContext::put);
            }

            // 4. 如果有 data list，轉換 ImageSource 並放入 context
            if (context.getData() != null && !context.getData().isEmpty()) {
                convertImageSourceInData(context.getData());
                velocityContext.put("items", context.getData());
            }

            // 5. 註冊圖片到 Velocity context
            registerImageProviders(velocityContext, context.getImages());

            // 6. 根據輸出格式產製
            if (context.getOutputFormat() == OutputFormat.PDF) {
                Options options = Options.getTo(ConverterTypeTo.PDF);
                report.convert(velocityContext, options, out);
            } else {
                report.process(velocityContext, out);
            }

        } catch (IOException | XDocReportException e) {
            throw new IllegalStateException("Failed to generate xDocReport", e);
        }

        // 7. 後處理：用 POI 插入頂層圖片（如 logo）
        if (context.getImages() != null && !context.getImages().isEmpty()
                && context.getOutputFormat() == OutputFormat.DOCX) {
            byte[] withImages = insertImagesWithPoi(out.toByteArray(), context.getImages());
            out.reset();
            try {
                out.write(withImages);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write image result", e);
            }
        }

        validateFileSize(out);

        String fileName = resolveFileName(context);
        String contentType = resolveContentType(context.getOutputFormat());

        log.info("<-- xDocReport generate | fileName={}, size={} bytes",
                fileName, out.size());
        return new ReportResult(out.toByteArray(), contentType, fileName);
    }

    /**
     * 用 POI 後處理：找到「圖片書籤」段落，插入真實圖片取代純文字。
     *
     * <p>xDocReport 的 addFieldAsImage 對 POI 程式化產生的範本不可靠，
     * 所以改用後處理方式：xDocReport 先做 Velocity 替換，
     * 再用 POI 打開結果 docx，找到空段落（原 $logo 位置已被清空），插入圖片。
     */
    private byte[] insertImagesWithPoi(byte[] docxBytes, Map<String, ImageSource> images) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            for (Map.Entry<String, ImageSource> entry : images.entrySet()) {
                ImageSource src = entry.getValue();
                byte[] imgBytes = src.resolveContent();
                int width = src.getWidth() != null ? src.getWidth() : 200;
                int height = src.getHeight() != null ? src.getHeight() : 50;

                // 找第一個空段落（xDocReport 替換 $logo 後變成空的），插入圖片
                for (XWPFParagraph para : doc.getParagraphs()) {
                    if (para.getText().isBlank() && para.getRuns().isEmpty()) {
                        para.setAlignment(ParagraphAlignment.CENTER);
                        XWPFRun run = para.createRun();
                        run.addPicture(new ByteArrayInputStream(imgBytes),
                                XWPFDocument.PICTURE_TYPE_PNG,
                                entry.getKey() + ".png",
                                Units.toEMU(width), Units.toEMU(height));
                        break;
                    }
                }
            }

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            doc.write(result);
            return result.toByteArray();
        } catch (Exception e) {
            log.warn("Failed to insert images via POI, returning original docx", e);
            return docxBytes;
        }
    }

    @Override
    public ReportResult generateMerged(List<ReportContext> contexts) {
        if (contexts.size() == 1) {
            return generate(contexts.getFirst());
        }

        log.info("--> xDocReport generateMerged | count={}", contexts.size());

        // 合併所有 context 的 parameters 和 images，data 以 items_0, items_1 分別放入
        ReportContext first = contexts.getFirst();

        Map<String, Object> mergedParams = new HashMap<>();
        Map<String, ImageSource> mergedImages = new java.util.LinkedHashMap<>();

        for (int i = 0; i < contexts.size(); i++) {
            ReportContext ctx = contexts.get(i);
            if (ctx.getParameters() != null) {
                mergedParams.putAll(ctx.getParameters());
            }
            if (ctx.getImages() != null) {
                mergedImages.putAll(ctx.getImages());
            }
            if (ctx.getData() != null && !ctx.getData().isEmpty()) {
                mergedParams.put("items_" + i, ctx.getData());
            }
        }

        // 用第一個 context 的 template 和 format 產製
        ReportContext merged = ReportContext.builder()
                .templatePath(first.getTemplatePath())
                .engineType(first.getEngineType())
                .outputFormat(first.getOutputFormat())
                .fileName(first.getFileName())
                .parameters(mergedParams)
                .images(mergedImages)
                .build();

        return generate(merged);
    }

    private FieldsMetadata getOrCreateMetadata(IXDocReport report) {
        FieldsMetadata metadata = report.getFieldsMetadata();
        if (metadata == null) {
            metadata = report.createFieldsMetadata();
        }
        return metadata;
    }

    /**
     * 註冊圖片欄位到 FieldsMetadata
     */
    private void registerImageMetadata(IXDocReport report,
                                       Map<String, ImageSource> images) throws XDocReportException {
        if (images == null || images.isEmpty()) {
            return;
        }
        FieldsMetadata metadata = getOrCreateMetadata(report);
        for (String fieldName : images.keySet()) {
            metadata.addFieldAsImage(fieldName);
        }
    }

    /**
     * 註冊 list 資料欄位到 FieldsMetadata，讓表格行自動重複。
     * 如果欄位值是 ImageSource，同時註冊為 image。
     */
    @SuppressWarnings("unchecked")
    private void registerListMetadata(IXDocReport report, List<?> data) throws XDocReportException {
        if (data == null || data.isEmpty()) {
            return;
        }
        FieldsMetadata metadata = getOrCreateMetadata(report);
        Object first = data.getFirst();
        if (first instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) first;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = "items." + entry.getKey();
                if (entry.getValue() instanceof ImageSource) {
                    // 圖片欄位：同時註冊為 list + image
                    metadata.addFieldAsImage(fieldName, fieldName);
                } else {
                    metadata.addFieldAsList(fieldName);
                }
            }
        } else {
            metadata.load("items", first.getClass(), true);
        }
    }

    /**
     * 將 data list 裡的 ImageSource 值轉成 IImageProvider，
     * 讓 xDocReport 能正確處理欄位圖片。
     */
    @SuppressWarnings("unchecked")
    private void convertImageSourceInData(List<?> data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        for (Object item : data) {
            if (item instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) item;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getValue() instanceof ImageSource imgSrc) {
                        byte[] bytes = imgSrc.resolveContent();
                        IImageProvider provider = new ByteArrayImageProvider(bytes);
                        if (imgSrc.getWidth() != null) {
                            provider.setWidth(imgSrc.getWidth().floatValue());
                        }
                        if (imgSrc.getHeight() != null) {
                            provider.setHeight(imgSrc.getHeight().floatValue());
                        }
                        entry.setValue(provider);
                    }
                }
            }
        }
    }

    /**
     * 將圖片 IImageProvider 放入 Velocity context
     */
    private void registerImageProviders(IContext velocityContext,
                                        Map<String, ImageSource> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (Map.Entry<String, ImageSource> entry : images.entrySet()) {
            byte[] imgBytes = entry.getValue().resolveContent();
            IImageProvider provider = new ByteArrayImageProvider(imgBytes);

            if (entry.getValue().getWidth() != null) {
                provider.setWidth(entry.getValue().getWidth().floatValue());
            }
            if (entry.getValue().getHeight() != null) {
                provider.setHeight(entry.getValue().getHeight().floatValue());
            }

            velocityContext.put(entry.getKey(), provider);
        }
    }

    private void validateFileSize(ByteArrayOutputStream out) {
        if (out.size() > MAX_FILE_SIZE) {
            throw new IllegalStateException(
                    "Report file exceeds maximum allowed size: 50MB");
        }
    }

    private InputStream loadTemplate(String templatePath) {
        // 先嘗試從 classpath 載入
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(templatePath);
        if (stream != null) {
            return stream;
        }
        // 嘗試從 file system 載入
        try {
            return new FileInputStream(templatePath);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(
                    "Template not found: " + templatePath, e);
        }
    }

    private String resolveFileName(ReportContext context) {
        if (context.getFileName() != null && !context.getFileName().isBlank()) {
            return context.getFileName();
        }
        return "report" + getExtension(context.getOutputFormat());
    }

    private String getExtension(OutputFormat format) {
        return switch (format) {
            case DOCX -> ".docx";
            case PDF -> ".pdf";
            case ODT -> ".odt";
            default -> ".docx";
        };
    }

    private String resolveContentType(OutputFormat format) {
        return switch (format) {
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case PDF -> "application/pdf";
            case ODT -> "application/vnd.oasis.opendocument.text";
            default -> "application/octet-stream";
        };
    }
}
