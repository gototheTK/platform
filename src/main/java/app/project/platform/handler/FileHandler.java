package app.project.platform.handler;

import app.project.platform.domain.type.ErrorCode;
import app.project.platform.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class FileHandler {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/images/";

    public String saveFile(MultipartFile file) {

        if (file.isEmpty()) return null;

        try {

            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdir();
            }

            String originalFilename = file.getOriginalFilename();
            String saveFileName = UUID.randomUUID() + "_" + originalFilename;

            File saveFile = new File(UPLOAD_DIR + saveFileName);
            file.transferTo(saveFile);

            return saveFileName;

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

    }

    public void deleteFile(String fileName) {

        if (fileName == null || fileName.isEmpty()) return;

        File file = new File(UPLOAD_DIR + fileName);
        if (file.exists()) {
            file.delete();
        }

    }

}
