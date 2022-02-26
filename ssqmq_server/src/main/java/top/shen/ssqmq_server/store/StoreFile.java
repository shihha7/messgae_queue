package top.shen.ssqmq_server.store;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.shen.ssqmq_server.core.Constants;
import top.shen.ssqmq_server.core.Message;

import java.util.concurrent.locks.ReentrantLock;

public class StoreFile {

    private Logger logger = LoggerFactory.getLogger(StoreFile.class);

    private int readIndexPos;

    private int readDataPos;

    private int readDataOffset;

    private int indexLimit;

    private int dataPos;

    private int dataOffset;

    /**
     * 0 空文件
     * 1 正常使用
     * 2 满文件
     */
    private int state;

    private MessageFile indexFile;

    private MessageFile dataFile;

    private String queueName;

    private String fileName;

    private StoreFile() {

    }

    public StoreFile(String queueName, String fileName) {
        this.queueName = queueName;
        this.fileName = fileName;
        initIndexFile();
        initDataFile();
    }

    private void initIndexFile() {
        MessageFile indexFile = new MessageFile(Constants.Store.getIndexDir(),this.queueName,this.fileName,Constants.Store.maxIndexSize);

        String stateIndexPosString = indexFile.getMessage(Constants.Store.stateIndexPos, Constants.Store.stateIndexOffset);
        if (stateIndexPosString == null || stateIndexPosString.equals("")) {
            this.state = 0;
            indexFile.addMessage(Constants.Store.stateIndexPos,(0+"").getBytes());
        } else {
            this.state = Integer.parseInt(stateIndexPosString);
        }

        this.indexFile = indexFile;

        initRead();
        initLimit();
    }

    private void initDataFile () {
        this.dataFile = new MessageFile(Constants.Store.getDataDir(),this.queueName,this.fileName,Constants.Store.maxDataSize);
    }

    private void initRead () {

        MessageFile indexFile = this.indexFile;

        int state = this.state;

        if (state == 0) {

            indexFile.addMessage(Constants.Store.readIndexPos,(Constants.Store.headRam +"").getBytes());

            this.readDataPos = 0;
            this.readDataOffset = 0;
            this.readIndexPos = Constants.Store.headRam;
        } else {

            String readIndexPosString = indexFile.getMessage(Constants.Store.readIndexPos, Constants.Store.readIndexOffset);
            int readIndexPos = Integer.parseInt(readIndexPosString);

            String readDataPos = indexFile.getMessage(readIndexPos, Constants.Store.indexMaxOffsetLeft);
            String readDataOffset = indexFile.getMessage(readIndexPos + Constants.Store.indexMaxOffsetLeft,Constants.Store.indexMaxOffsetRight);

            this.readDataPos = readDataPos == null || readDataPos.trim().equals("") ? 0 : Integer.parseInt(readDataPos);
            this.readDataOffset = readDataOffset == null || readDataOffset.trim().equals("") ? 0 : Integer.parseInt(readDataOffset);
            this.readIndexPos = readIndexPos;
        }


    }

    private void initLimit() {

        MessageFile indexFile = this .indexFile;
        int state = this.state;

        String indexLimitString = indexFile.getMessage(Constants.Store.limitIndexPos, Constants.Store.limitIndexOffset);

        if (state == 0) {
            int initLimit = Constants.Store.headRam;
            this.dataPos = 0;
            this.dataOffset = 0;
            this.indexLimit = initLimit;
        } else {
            int indexLimit = Integer.parseInt(indexLimitString);

            String dataPos = indexFile.getMessage(indexLimit - Constants.Store.indexMaxOffset, Constants.Store.indexMaxOffsetLeft);
            String dataOffset = indexFile.getMessage(indexLimit - Constants.Store.indexMaxOffsetRight,Constants.Store.indexMaxOffsetRight);

            this.dataPos = Integer.parseInt(dataPos);
            this.dataOffset = Integer.parseInt(dataOffset);
            this.indexLimit = indexLimit;
        }

    }


    public void addMessage(String message) {


        final MessageFile indexFile = this.indexFile;

        final MessageFile dateFile = this.dataFile;

        if (this.state == 0) {
            this.state = 1;
            indexFile.addMessage(Constants.Store.stateIndexPos,(1+"").getBytes());
        }

        int curIndexPos = this.indexLimit;

        int preDataPos = this.dataPos;
        int preDataOffset = this.dataOffset;

        int curDataPos = preDataPos + preDataOffset;
        int curDataOffset = message.getBytes().length;

        dateFile.addMessage(curDataPos,message.getBytes());

        int curIndexLimit = curIndexPos + Constants.Store.indexMaxOffset;

        indexFile.addMessage(curIndexPos,("" + curDataPos).getBytes());
        indexFile.addMessage(curIndexPos + Constants.Store.indexMaxOffsetLeft , ("" + curDataOffset).getBytes());
        indexFile.addMessage(Constants.Store.limitIndexPos,("" + curIndexLimit).getBytes() );

        this.indexLimit = curIndexLimit;
        this.dataPos = curDataPos;
        this.dataOffset = curDataOffset;

    }

    public String pollMessage() {

        final MessageFile indexFile = this.indexFile;

        final MessageFile dateFile = this.dataFile;

        int readIndexPos = this.readIndexPos;
        int readDataPos = this.readDataPos;
        int readDataOffset = this.readDataOffset;
        int indexLimit = this.indexLimit;

        int nextIndexPos = readIndexPos + Constants.Store.indexMaxOffset;

        if (readDataOffset == 0) {
           return "";
        }

        String message = dateFile.getMessage(readDataPos, readDataOffset);
        if (!checkJson(message)) {
            this.readDataOffset = 0;
            return "";
        }

        indexFile.addMessage(Constants.Store.readIndexPos,(nextIndexPos+"").getBytes());

        this.readIndexPos = nextIndexPos;
        this.readDataPos = readDataPos + readDataOffset;
        if (nextIndexPos >= indexLimit) {
            this.readDataOffset = 0;
        } else {
            String nextDataOffset = indexFile.getMessage(nextIndexPos + Constants.Store.indexMaxOffsetLeft, Constants.Store.indexMaxOffsetRight);
            if (nextDataOffset.equals("")) {
                this.readDataOffset = 0;
            } else {
                this.readDataOffset = Integer.parseInt(nextDataOffset);
            }
        }

        return message;

    }

    public int checkPollMessage(){

        final MessageFile indexFile = this.indexFile;
        final MessageFile dataFile = this.dataFile;

        int state = this.state;
        int readDataOffset = this.readDataOffset;
        int readIndexPos = this.readIndexPos;
        int indexLimit = this.indexLimit;

        int nextIndexPos = readIndexPos + Constants.Store.indexMaxOffset;


        if (readDataOffset == 0) {
            if (nextIndexPos >= indexLimit && state == 2) {
                return Constants.Offset.FULL;
            } else {
                if (indexFile.checkSurpassSize(readIndexPos+Constants.Store.indexMaxOffset)) {
                    return Constants.Offset.FULL;
                }
                String curDataOffset = indexFile.getMessage(readIndexPos + Constants.Store.indexMaxOffsetLeft, Constants.Store.indexMaxOffsetRight);
                if (!"".equals(curDataOffset)) {
                    readDataOffset = Integer.parseInt(curDataOffset);
                    this.readDataOffset = readDataOffset;
                }
            }
        }
        return readDataOffset > 0 ? Constants.Offset.SUCCESS:Constants.Offset.NULL;
    }

    public boolean checkCapacity(String message) {
        int state = this.state;
        int dataLimit = this.dataPos + this.dataOffset;
        int indexLimit = this.indexLimit;
        if (state == 2) {
            return false;
        }

        boolean isDataCapacity = getDataFile().getFileSize() > dataLimit + message.getBytes().length;

        boolean isIndexCapacity = getIndexFile().getFileSize() > indexLimit + Constants.Store.indexMaxOffset;

        if (!isDataCapacity || !isIndexCapacity) {
            MessageFile indexFile = this.indexFile;
            indexFile.addMessage(Constants.Store.stateIndexPos,(2+"").getBytes());
            this.state = 2;
        }
        return  isDataCapacity && isIndexCapacity;
    }

    public boolean checkPollAllAndClose(){

        int state = this.state;
        int readIndexPos = this.readIndexPos;
        int indexLimit = this.indexLimit;
        if (state == 2 && readIndexPos>=indexLimit) {
            close();
            logger.info("删除文件 {} {} {}",fileName,readIndexPos, indexLimit);
            return true;
        }
        return false;
    }


    public boolean checkJson(String message) {
        try {
            JSONObject.parseObject(message, Message.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void close(){
        this.indexFile.close();
        this.dataFile.close();
    }

    public MessageFile getIndexFile() {
        return indexFile;
    }

    public void setIndexFile(MessageFile indexFile) {
        this.indexFile = indexFile;
    }

    public MessageFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(MessageFile dataFile) {
        this.dataFile = dataFile;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
