package top.shen.ssqmq_server.core;


import top.shen.ssqmq_server.utils.PropertiesUtil;

public class Constants {


    public static final class Type {
        public static final int CONNECT = 1;
        public static final int HEART_BEAT = 2;
        public static final int SEND = 3;
    }

    public static final class Store {


        private static final String dataDirKey = "ssqmq.server.datadir";
        private static String dataDir;

        private static final String indexDirKey = "ssqmq.server.indexdir";
        private static String indexDir;

        public static final int maxDataSize = 1024 * 256;
        public static final int maxIndexSize = 1024 * 16;
        public static final int indexMaxOffsetLeft = 10;
        public static final int indexMaxOffsetRight = 10;
        public static final int indexMaxOffset = indexMaxOffsetLeft + indexMaxOffsetRight;


        public static final int readIndexPos = 0;
        public static final int readIndexOffset = 10;
        public static final int limitIndexPos = readIndexOffset;
        public static final int limitIndexOffset = 10;

        public static final int stateIndexPos = readIndexOffset + limitIndexPos;
        public static final int stateIndexOffset = 10;

        public static final int headRam = readIndexOffset + limitIndexOffset + stateIndexOffset;
        public static final String fileSuffix = ".ssq";

        public static String getDataDir() {
            return dataDir = dataDir == null || dataDir.trim().equals("") ? PropertiesUtil.getProperties(dataDirKey) : dataDir;
        }

        public static String getIndexDir() {
            return indexDir = indexDir == null || indexDir.trim().equals("") ? PropertiesUtil.getProperties(indexDirKey) : indexDir;
        }

    }

    public static final class Offset {

        public static final int FULL = 1;
        public static final int NULL = 2;
        public static final int SUCCESS = 3;
    }
}
