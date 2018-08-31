package com.awesome.hydra.photobrowsewidget.storage;

import java.util.ArrayList;
import java.util.List;

public class PhotoList {
    private static final PhotoList INSTANCE = new PhotoList();

    public static PhotoList getInstance() {
        return INSTANCE;
    }

    public static List<String> photoGalleries = new ArrayList<>();
}
