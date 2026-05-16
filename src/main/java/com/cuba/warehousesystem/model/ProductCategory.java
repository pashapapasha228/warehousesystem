package com.cuba.warehousesystem.model;

public enum ProductCategory {
    ELECTRONICS("Электроника"),
    COMPONENTS("Комплектующие"),
    CABLES("Кабельная продукция"),
    TOOLS("Инструменты"),
    OFFICE("Офис"),
    CONSUMABLES("Расходники"),
    NETWORK("Сетевое оборудование"),
    SERVER("Серверное оборудование"),
    OTHER("Другое");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ProductCategory fromLabel(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        for (ProductCategory category : values()) {
            if (category.name().equalsIgnoreCase(value) || category.label.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return OTHER;
    }
}
