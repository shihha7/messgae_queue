package top.shen.ssqmq_server.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.shen.ssqmq_server.core.Constants;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class MessageFileQueue {

    private Logger logger = LoggerFactory.getLogger(MessageFileQueue.class);

    private ConcurrentHashMap<String, LinkedBlockingQueue<StoreFile>> storeFileMap = new ConcurrentHashMap<>();

    private ReentrantLock addLock = new ReentrantLock();

    private ReentrantLock pollLock = new ReentrantLock();

    @PostConstruct
    public void init () {

        File fileIndexDir = checkDir(Constants.Store.getIndexDir());

        File[] fileIndex = fileIndexDir.listFiles((i) -> i.isDirectory());

        if (fileIndex != null) {
            for (int i = 0; i < fileIndex.length; i++) {
                String queueName = fileIndex[i].getName();

                LinkedBlockingQueue<StoreFile> queue = new LinkedBlockingQueue<>();

                List<File> storeFileList = sortAndList(fileIndex[i].listFiles(t -> t.isFile()));
                for ( int j = 0 ; j < storeFileList.size() ; j++) {
                    StoreFile storeFile = new StoreFile(queueName,storeFileList.get(i).getName());
                    queue.offer(storeFile);
                }
                storeFileMap.put(queueName,queue);
            }
        }

        logger.info("init finish");

    }

    private File checkDir (String dir) {
        File fileDir = new File(dir);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        return fileDir;
    }

    private List<File> sortAndList(File [] files){
        List<File> list =  Arrays.asList(files);
        Collections.sort(list, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                String o1Name = o1.getName().replace(Constants.Store.fileSuffix, "");
                String o2Name = o2.getName().replace(Constants.Store.fileSuffix, "");
                return (int)(Long.parseLong(o1Name) - Long.parseLong(o2Name));
            }
        });

        return list;
    }

    public void addMessage(String queueName,String message) {

        if (!checkParam(queueName,message)) {
            logger.error("message is null queueName->{}, message->{}",queueName,message);
            return;
        }

        this.addLock.lock();
        try {
            LinkedBlockingQueue<StoreFile> storeFiles = storeFileMap.get(queueName);
            if (storeFiles == null) {
                this.storeFileMap.put(queueName,new LinkedBlockingQueue<>());
                storeFiles = storeFileMap.get(queueName);
            }

            StoreFile lastStoreFile = lastStoreFile(storeFiles,queueName);

            if (lastStoreFile == null) {
                lastStoreFile = createStoreFile(queueName);
                storeFiles.offer(lastStoreFile);
                boolean ss = storeFileMap.get(queueName).size() ==storeFiles.size()?true:false;
            }

            if (!lastStoreFile.checkCapacity(message)) {
                StoreFile newStoreFile = createStoreFile(queueName);
                storeFiles.offer(newStoreFile);
                lastStoreFile = newStoreFile;
            }
            lastStoreFile.addMessage(message);
        } finally {
            this.addLock.unlock();
        }

    }


    public String pollMessage(String queueName) {

        LinkedBlockingQueue<StoreFile> storeFiles = storeFileMap.get(queueName);

        if (storeFiles == null || storeFiles.size() == 0) {
            return null;
        }

        StoreFile storeFile = storeFiles.peek();

        if (storeFile == null) {
            return null;
        }

        this.pollLock.lock();

        try {
            int offsetState = storeFile.checkPollMessage();

            switch (offsetState) {
                case Constants.Offset.FULL:
                    storeFiles.poll();
                    storeFile = storeFiles.peek();
                    if (storeFile == null) {
                        return null;
                    }
                    break;
                case Constants.Offset.SUCCESS:
                    break;
                case Constants.Offset.NULL:
                default:
                    return null;
            }

            String msg = storeFile.pollMessage();

            if (storeFile.checkPollAllAndClose()){
                storeFiles.poll();
            }
            return msg;

        } finally {
            this.pollLock.unlock();
        }
    }


    private StoreFile lastStoreFile(LinkedBlockingQueue<StoreFile> storeFiles,String queueName) {

        StoreFile storeFile = null;

        if (storeFiles.isEmpty()) {
            return storeFile;
        }
        Iterator<StoreFile> iterator = storeFiles.iterator();
        while (iterator.hasNext()) {
            storeFile = iterator.next();
        }

        return storeFile;
    }

    private StoreFile createStoreFile(String queueName) {
        String fileName = System.currentTimeMillis()+Constants.Store.fileSuffix;
        return new StoreFile(queueName,fileName);
    }


    private boolean checkParam (String queueName,String message) {
        if (queueName==null || queueName.trim().equals("") || message == null || message.trim().equals("")) {
            return  false;
        }
        return true;
    }
}
