package com.skodev.FreeAS2;

import com.skodev.ConfigReader.ConfigReader;
import com.skodev.exceptions.AS2Exception;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreeAS2Communicator {

    private Map<String, File> outboundMap;
    private Map<File, String> inboundMap;
    private FileAlterationMonitor fam;
    private static Logger log = LoggerFactory.getLogger(FreeAS2Communicator.class.getName());
    private static double outcomeCntr = 0;

    public FreeAS2Communicator(long checkNewMsgsIntervalMlsec, IncomingMsgsListener inboundListener) throws AS2Exception {
        try {
            outboundMap = new HashMap<>();
            inboundMap = new HashMap<>();
            this.retrieveInOutDirsFromConfig();

            fam = new FileAlterationMonitor(checkNewMsgsIntervalMlsec);
            for (File f : inboundMap.keySet()) {
                FileFilter filter = new OnlyRootDirFileFilter(f);
                FileAlterationObserver fao = new FileAlterationObserver(f, filter);
                fao.addListener(new CreateNewFileOnlyListenerAdapter(inboundListener));
                fam.addObserver(fao);
                log.info("New inbound listener had been started on directory: {}", f);
            }
            fam.start();
        } catch (Exception ex) {
            throw new AS2Exception(ex);
        }
    }

    private void retrieveInOutDirsFromConfig() throws AS2Exception {
        List<ConfigReader.Partner> partners = ConfigReader.getParnersInfo();

        for (ConfigReader.Partner p : partners) {
            File inD = new File(p.inDir);
            if (inD.mkdirs()) {
                log.info("New dirictory {} for inbound messages was created", inD);
            }
            File outD = new File(p.outDir);
            if (outD.mkdirs()) {
                log.info("New dirictory {} for outbound messages was created", outD);
            }
            if (this.outboundMap.containsValue(outD)) {
                throw new AS2Exception(" Wron ASConfig.xml: directory " + outD + " had already been registered for other partner.");
            }
            this.inboundMap.put(inD, p.GLN);
            this.outboundMap.put(p.GLN, outD);
            log.info("New AS2 party with GLN = {} regisered. Inbound dir: {}, outbound: {}", p.GLN, p.inDir, p.outDir);
        }
    }

    public void sendMsg(File f, String receiverGLN) throws AS2Exception  {
        for (String gln : outboundMap.keySet()) {
            if (gln.equals(receiverGLN)) {
                try {
                    //File outboundDir = outboundMap.get(gln);
                    //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                    //String msgFileName = timeStamp + "_" + String.valueOf(outcomeCntr) + ".edi";
                    outcomeCntr++;
                    
                    //FileUtils.moveFileToDirectory(f, outboundDir, true);
                    FileUtils.moveFileToDirectory(f, new File("D:\\Files\\11sem\\Magistr\\Projects\\VTigerAS2EDIntegration\\OutMappingTest"), true);
                    
                    //FileUtils.writeStringToFile(new File(outboundDir, msgFileName), msg);
                    return;
                } catch (IOException ex) {
                    throw new AS2Exception("Sending file"+ f.getName() +"fail", ex);
                }
            }
        }
    }

    private class OnlyRootDirFileFilter implements FileFilter {

        public OnlyRootDirFileFilter(File rootDir) {
            this.rootDir = rootDir;
        }

        private File rootDir;

        @Override
        public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getParentFile().equals(rootDir);
        }
    };
}
