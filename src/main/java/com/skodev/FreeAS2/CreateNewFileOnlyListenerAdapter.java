package com.skodev.FreeAS2;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateNewFileOnlyListenerAdapter implements FileAlterationListener {

    private IncomingMsgsListener iml;
    private static final Logger log = LoggerFactory.getLogger(CreateNewFileOnlyListenerAdapter.class);

    public CreateNewFileOnlyListenerAdapter(IncomingMsgsListener iml) {
        this.iml = iml;
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
    }

    @Override
    public void onDirectoryCreate(File directory) {
    }

    @Override
    public void onDirectoryChange(File directory) {
    }

    @Override
    public void onDirectoryDelete(File directory) {
    }

    @Override
    public void onFileCreate(File file) {
        try {
            File processed = new File(file.getParent(), "processed");
            File newDis = new File(processed.getPath(), file.getName());
            FileUtils.moveFileToDirectory(file, processed, true);
            System.out.println(newDis);
            iml.newInbound(newDis);
        } catch (IOException ex) {
            log.error("Exception in file listener adapter.", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onFileChange(File file) {
    }

    @Override
    public void onFileDelete(File file) {
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
    }

}
