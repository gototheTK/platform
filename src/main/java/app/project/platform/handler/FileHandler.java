package app.project.platform.handler;

import app.project.platform.entity.Content;
import app.project.platform.entity.ContentImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class FileHandler {

    @Value("${file.dir}")
    private String fileDir;

    public String getFullPath(String filename) {
        return fileDir + filename;
    }

    public ContentImage storeFile(MultipartFile multipartFile, Content content) throws IOException {

        if (multipartFile.isEmpty()) return null;

        String originalFilename = multipartFile.getOriginalFilename();

        String storeFilename = UUID.randomUUID() + "-" + getFullPath(originalFilename);

        // 디스크에 저장
        multipartFile.transferTo(new File(storeFilename));

        return ContentImage.builder()
                .originalFileName(originalFilename)
                .storeFilename(storeFilename)
                .content(content)
                .build();
    }

    public void deleteFile(String storeFilename) {
        File file = new File(getFullPath(storeFilename));
        if (file.exists()) {
            file.delete();
        }
    }

    private String extractExt(String originalFilename) {
        int pos = originalFilename.lastIndexOf(".");
        return originalFilename.substring(pos+1);
    }

}