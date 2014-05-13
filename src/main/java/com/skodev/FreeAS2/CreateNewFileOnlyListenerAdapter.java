package com.skodev.FreeAS2;

import java.io.File;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class CreateNewFileOnlyListenerAdapter implements FileAlterationListener{

    private IncomingMsgsListener iml;

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
        iml.newInbound(file);
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
