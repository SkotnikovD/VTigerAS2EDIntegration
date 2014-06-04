package com.skodev.ConfigReader;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class ConfigReader {

    public static class Partner {

        public String name;
        public String GLN;
        public String inDir;
        public String outDir;

        public Partner(String name, String GLN, String inDir, String outDir) {
            this.name = name;
            this.GLN = GLN;
            this.inDir = inDir;
            this.outDir = outDir;
        }
    }
    private static Document document;

    static {
        InputStream in = ConfigReader.class.getResourceAsStream("/AS2Config.xml");
        SAXReader reader = new SAXReader();
        try {
            document = reader.read(in);
        } catch (DocumentException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static List<Partner> getParnersInfo() {

        List<Partner> l = new LinkedList<>();

        Element root = document.getRootElement();
        List<Element> partnersList = root.element("partners").elements("partner");

        for (Element e : partnersList) {
            String name = e.element("name").getText();
            String GLN = e.element("GLN").getText();
            String inDir = e.element("inbound_dir").getText();
            String outDir = e.element("outbound_dir").getText();
            l.add(new Partner(name, GLN, inDir, outDir));
        }
        return l;
    }

    public static String getMyGLN() {
        Element root = document.getRootElement();
        return root.element("internals").element("GLN").getText();
    }
}
