package top.shen.ssqmq_server.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MessageFile {

    private Logger logger = LoggerFactory.getLogger(MessageFile.class);

    private MappedByteBuffer mappedByteBuffer;

    private FileChannel fileChannel;

    private RandomAccessFile randomAccessFile;

    private File file;

    private String queueName;

    private String fileName;

    private int fileSize;

    private String dir;

    private MessageFile() {
    }

    public MessageFile(String dir,String queueName,String fileName, int fileSize) {

        this.queueName = queueName;
        this.dir = dir = dir + queueName + File.separator;
        checkDir();

        this.fileName = fileName;
        this.fileSize = fileSize;
        this.file = new File(dir+fileName);
        init();
    }


    private void init() {

        //没有初始化成功要关闭
        boolean success = false;

        try {
            this.randomAccessFile = new RandomAccessFile(this.file,"rw");
            this.fileChannel = this.randomAccessFile.getChannel();
            this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE,0,this.fileSize);

            success = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.error("FileNotFoundException filename->{}",this.fileName);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("fileChannel map  error filename->{}",this.fileName);
        } finally {
            if (!success && this.fileChannel != null) {
                try {
                    this.fileChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (!success && this.randomAccessFile != null) {
                try {
                    this.randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }


    public void addMessage(int position , byte [] bytes){

        for (int i = 0; i < bytes.length; i++) {
            this.mappedByteBuffer.put(i + position,bytes[i]);
        }

    }

    public String getMessage(int position , int offset){
        if (checkSurpassSize(position+offset)) {
            return "";
        }

        byte [] res = new byte[offset];

        for (int i = 0; i < offset; i++) {
            res[i] = this.mappedByteBuffer.get(position+i);
        }
        return new String(res).trim();
    }

    public void close() {

        logger.info("start file close ->{}",this.fileName);

        this.mappedByteBuffer = null;
        try {
            this.fileChannel.close();
            this.randomAccessFile.close();
            this.file.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }


        logger.info("end file close ->{}",this.fileName);
    }


    private void checkDir() {
        if (this.dir != null) {
            File dir = new File(this.dir);
            if (!dir.exists()) {
                boolean flag = dir.mkdirs();

                if (flag) {
                    logger.error("mkdir error");
                } else {
                    logger.info("mkdirs succcess");
                }
            }
        }
    }

    public MappedByteBuffer getMappedByteBuffer() {
        return this.mappedByteBuffer;
    }

    public FileChannel getFileChannel() {
        return this.fileChannel;
    }

    public RandomAccessFile getRandomAccessFile() {
        return this.randomAccessFile;
    }

    public File getFile() {
        return this.file;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getFileSize() {
        return this.fileSize;
    }

    public boolean checkSurpassSize(int val){
        return val > this.fileSize;
    }
}










