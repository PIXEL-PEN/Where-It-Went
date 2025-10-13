package com.pixelpen.whereitwent;

public class CategoryItem {
    public String name;
    public String tagType;

    public CategoryItem(String name, String tagType) {
        this.name = name;
        this.tagType = tagType;
    }

    @Override
    public String toString() {
        return name;
    }
}
