package com.company.common.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuditableEntity 審計基礎實體驗證
 *
 * 確保四個審計欄位的 JPA 註解與 Spring Data Auditing 標記正確。
 */
@DisplayName("AuditableEntity 審計基礎實體")
class AuditableEntityTest {

    /**
     * 測試用具體子類（AuditableEntity 是 abstract）
     */
    static class TestAuditableEntity extends AuditableEntity {
    }

    @Nested
    @DisplayName("類別層級 — JPA 與 Auditing 設定")
    class ClassLevelAnnotations {

        @Test
        @DisplayName("標記為 @MappedSuperclass，不會產生獨立資料表")
        void 標記為MappedSuperclass() {
            assertThat(AuditableEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();
        }

        @Test
        @DisplayName("使用 AuditingEntityListener 自動填入審計欄位")
        void 使用AuditingEntityListener() {
            EntityListeners annotation = AuditableEntity.class.getAnnotation(EntityListeners.class);
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).contains(AuditingEntityListener.class);
        }

        @Test
        @DisplayName("實作 Serializable 介面")
        void 實作Serializable() {
            assertThat(Serializable.class.isAssignableFrom(AuditableEntity.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("createdDate 欄位")
    class CreatedDateField {

        @Test
        @DisplayName("標記 @CreatedDate，自動填入建立時間")
        void 標記CreatedDate() throws NoSuchFieldException {
            Field field = AuditableEntity.class.getDeclaredField("createdDate");
            assertThat(field.isAnnotationPresent(CreatedDate.class)).isTrue();
        }

        @Test
        @DisplayName("欄位映射為 created_date，且不可更新")
        void 欄位映射正確且不可更新() throws NoSuchFieldException {
            Field field = AuditableEntity.class.getDeclaredField("createdDate");
            Column column = field.getAnnotation(Column.class);
            assertThat(column.name()).isEqualTo("created_date");
            assertThat(column.updatable()).isFalse();
        }

        @Test
        @DisplayName("欄位型別為 LocalDateTime")
        void 型別為LocalDateTime() throws NoSuchFieldException {
            Field field = AuditableEntity.class.getDeclaredField("createdDate");
            assertThat(field.getType()).isEqualTo(LocalDateTime.class);
        }
    }

    @Nested
    @DisplayName("lastModifiedDate 欄位")
    class LastModifiedDateField {

        @Test
        @DisplayName("標記 @LastModifiedDate，每次更新自動填入")
        void 標記LastModifiedDate() throws NoSuchFieldException {
            Field field = AuditableEntity.class.getDeclaredField("lastModifiedDate");
            assertThat(field.isAnnotationPresent(LastModifiedDate.class)).isTrue();
        }

        @Test
        @DisplayName("欄位映射為 last_modified_date")
        void 欄位映射正確() throws NoSuchFieldException {
            Field field = AuditableEntity.class.getDeclaredField("lastModifiedDate");
            Column column = field.getAnnotation(Column.class);
            assertThat(column.name()).isEqualTo("last_modified_date");
        }
    }

    @Nested
    @DisplayName("createdBy 欄位")
    class CreatedByField {

        @Test
        @DisplayName("標記 @CreatedBy，自動填入建立人")
        void 標記CreatedBy() throws NoSuchFieldException {
            Field field = AuditableEntity.class.getDeclaredField("createdBy");
            assertThat(field.isAnnotationPresent(CreatedBy.class)).isTrue();
        }

        @Test
        @DisplayName("欄位映射為 created_by，且不可更新")
        void 欄位映射正確且不可更新() throws NoSuchFieldException {
            Field field = AuditableEntity.class.getDeclaredField("createdBy");
            Column column = field.getAnnotation(Column.class);
            assertThat(column.name()).isEqualTo("created_by");
            assertThat(column.updatable()).isFalse();
        }
    }

    @Nested
    @DisplayName("lastModifiedBy 欄位")
    class LastModifiedByField {

        @Test
        @DisplayName("標記 @LastModifiedBy，每次更新自動填入修改人")
        void 標記LastModifiedBy() throws NoSuchFieldException {
            Field field = AuditableEntity.class.getDeclaredField("lastModifiedBy");
            assertThat(field.isAnnotationPresent(LastModifiedBy.class)).isTrue();
        }

        @Test
        @DisplayName("欄位映射為 last_modified_by")
        void 欄位映射正確() throws NoSuchFieldException {
            Field field = AuditableEntity.class.getDeclaredField("lastModifiedBy");
            Column column = field.getAnnotation(Column.class);
            assertThat(column.name()).isEqualTo("last_modified_by");
        }
    }

    @Nested
    @DisplayName("Getter/Setter 行為")
    class GetterSetter {

        @Test
        @DisplayName("可正確設定與取得所有審計欄位")
        void 可設定與取得所有欄位() {
            TestAuditableEntity entity = new TestAuditableEntity();
            LocalDateTime now = LocalDateTime.now();

            entity.setCreatedDate(now);
            entity.setLastModifiedDate(now);
            entity.setCreatedBy("admin");
            entity.setLastModifiedBy("bob");

            assertThat(entity.getCreatedDate()).isEqualTo(now);
            assertThat(entity.getLastModifiedDate()).isEqualTo(now);
            assertThat(entity.getCreatedBy()).isEqualTo("admin");
            assertThat(entity.getLastModifiedBy()).isEqualTo("bob");
        }
    }
}
