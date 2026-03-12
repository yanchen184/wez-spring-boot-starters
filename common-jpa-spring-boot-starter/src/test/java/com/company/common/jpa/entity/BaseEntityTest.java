package com.company.common.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseEntity 完整基礎實體驗證
 *
 * 繼承 AuditableEntity，額外提供軟刪除與樂觀鎖功能。
 */
@DisplayName("BaseEntity 完整基礎實體（審計 + 軟刪除 + 樂觀鎖）")
class BaseEntityTest {

    /**
     * 測試用具體子類
     */
    static class TestBaseEntity extends BaseEntity {
    }

    @Nested
    @DisplayName("繼承關係")
    class Inheritance {

        @Test
        @DisplayName("繼承自 AuditableEntity，擁有四個審計欄位")
        void 繼承自AuditableEntity() {
            assertThat(AuditableEntity.class.isAssignableFrom(BaseEntity.class)).isTrue();
        }

        @Test
        @DisplayName("標記為 @MappedSuperclass")
        void 標記為MappedSuperclass() {
            assertThat(BaseEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("deleted 軟刪除欄位")
    class DeletedField {

        @Test
        @DisplayName("預設值為 false（未刪除）")
        void 預設值為false() {
            TestBaseEntity entity = new TestBaseEntity();
            assertThat(entity.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("欄位映射為 deleted，不可為 null")
        void 欄位映射正確() throws NoSuchFieldException {
            Field field = BaseEntity.class.getDeclaredField("deleted");
            Column column = field.getAnnotation(Column.class);
            assertThat(column.name()).isEqualTo("deleted");
            assertThat(column.nullable()).isFalse();
        }

        @Test
        @DisplayName("delete() 方法將 deleted 設為 true")
        void delete方法設為true() {
            TestBaseEntity entity = new TestBaseEntity();
            entity.delete();
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("restore() 方法將 deleted 恢復為 false")
        void restore方法恢復為false() {
            TestBaseEntity entity = new TestBaseEntity();
            entity.delete();
            entity.restore();
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("version 樂觀鎖欄位")
    class VersionField {

        @Test
        @DisplayName("標記 @Version，由 JPA 自動管理")
        void 標記Version() throws NoSuchFieldException {
            Field field = BaseEntity.class.getDeclaredField("version");
            assertThat(field.isAnnotationPresent(Version.class)).isTrue();
        }

        @Test
        @DisplayName("欄位映射為 version")
        void 欄位映射正確() throws NoSuchFieldException {
            Field field = BaseEntity.class.getDeclaredField("version");
            Column column = field.getAnnotation(Column.class);
            assertThat(column.name()).isEqualTo("version");
        }

        @Test
        @DisplayName("初始值為 null（由 JPA 在首次 persist 時設定）")
        void 初始值為null() {
            TestBaseEntity entity = new TestBaseEntity();
            assertThat(entity.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("軟刪除工作流程")
    class SoftDeleteWorkflow {

        @Test
        @DisplayName("新建 -> 刪除 -> 恢復 完整生命週期")
        void 完整生命週期() {
            TestBaseEntity entity = new TestBaseEntity();

            // 新建：未刪除
            assertThat(entity.isDeleted()).isFalse();

            // 刪除
            entity.delete();
            assertThat(entity.isDeleted()).isTrue();

            // 恢復
            entity.restore();
            assertThat(entity.isDeleted()).isFalse();
        }
    }
}
