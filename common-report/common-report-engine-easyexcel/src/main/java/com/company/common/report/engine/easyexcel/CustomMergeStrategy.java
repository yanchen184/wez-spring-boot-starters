package com.company.common.report.engine.easyexcel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.merge.AbstractMergeStrategy;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * 自訂合併策略：相同值自動垂直合併
 *
 * <p>指定要合併的欄位索引，當相鄰行的該欄位值相同時自動合併儲存格。
 * 適用於分組報表中需要合併相同分類的場景。
 *
 * <p>使用方式：
 * <pre>
 * EasyExcel.write(out, DataClass.class)
 *     .registerWriteHandler(new CustomMergeStrategy(0, 1))  // 合併第 0、1 欄
 *     .sheet("Sheet1")
 *     .doWrite(dataList);
 * </pre>
 */
public class CustomMergeStrategy extends AbstractMergeStrategy {

    /** 要合併的欄位索引 */
    private final int[] mergeColumnIndexes;

    /**
     * @param mergeColumnIndexes 要進行垂直合併的欄位索引（0-based）
     */
    public CustomMergeStrategy(int... mergeColumnIndexes) {
        this.mergeColumnIndexes = mergeColumnIndexes;
    }

    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (relativeRowIndex == null || relativeRowIndex == 0) {
            return;
        }

        int currentColumnIndex = cell.getColumnIndex();
        if (!shouldMerge(currentColumnIndex)) {
            return;
        }

        int currentRowIndex = cell.getRowIndex();
        String currentValue = getCellStringValue(cell);
        String previousValue = getCellStringValue(
                sheet.getRow(currentRowIndex - 1).getCell(currentColumnIndex));

        if (currentValue != null && currentValue.equals(previousValue)) {
            // 檢查是否已有包含上一行的合併區域
            boolean merged = false;
            for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
                CellRangeAddress range = sheet.getMergedRegion(i);
                if (range.getFirstColumn() == currentColumnIndex
                        && range.getLastColumn() == currentColumnIndex
                        && range.getLastRow() == currentRowIndex - 1) {
                    // 擴展現有合併區域
                    sheet.removeMergedRegion(i);
                    range = new CellRangeAddress(
                            range.getFirstRow(), currentRowIndex,
                            currentColumnIndex, currentColumnIndex);
                    sheet.addMergedRegion(range);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                // 建立新的合併區域
                CellRangeAddress range = new CellRangeAddress(
                        currentRowIndex - 1, currentRowIndex,
                        currentColumnIndex, currentColumnIndex);
                sheet.addMergedRegion(range);
            }
        }
    }

    private boolean shouldMerge(int columnIndex) {
        for (int index : mergeColumnIndexes) {
            if (index == columnIndex) {
                return true;
            }
        }
        return false;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        return null;
    }
}
