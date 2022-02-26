package top.shen.ssqmq_server.utils;

public class RandomUtil {

    /**
     * 随机取小于size的正征收
     * @param size
     * @return
     */
    public static int random(int size) {
        return  (int) (Math.random() * size);
    }

}
