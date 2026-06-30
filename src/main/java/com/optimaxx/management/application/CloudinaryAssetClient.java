package com.optimaxx.management.application;

import java.io.IOException;

public interface CloudinaryAssetClient {

    String upload(byte[] bytes, String contentType, String publicId) throws IOException;
}
