package app.project.platform.util;

import app.project.platform.domain.type.ContentCategory;
import app.project.platform.entity.Content;

import java.util.*;
import java.util.stream.Collectors;

public class SimilarityUtil {

    public static double calculateCosineSimilarity(Map<ContentCategory, Integer> v1,Map<ContentCategory, Integer> v2) {
        if (v1 == null || v2 == null || v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }

        // 차원 맞추기
        Set<ContentCategory> allKeys = new HashSet<>();
        allKeys.addAll(v1.keySet());
        allKeys.addAll(v2.keySet());

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (ContentCategory key : allKeys) {

            int val1 = v1.getOrDefault(key, 0);
            int val2 = v2.getOrDefault(key, 0);

            dotProduct += val1 * val2;
            norm1 += Math.pow(val1, 2);
            norm2 += Math.pow(val2, 2);
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct/ (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public static double calculatePopularityWithLogScaling(double likeCount, double maxLikeLog) {
        if (maxLikeLog <= 0.0) return 0.0;

        return Math.log1p(likeCount) / maxLikeLog;
    }

    public static double calculateEuclidDistanceFromIdealStatus(double x, double y, double z) {

        double dx = Math.pow(1.0 - x, 2);
        double dy = Math.pow(1.0 - y, 2);
        double dz = Math.pow(1.0 - z, 2);

        return Math.sqrt(dx + dy + dz);

    }

}
