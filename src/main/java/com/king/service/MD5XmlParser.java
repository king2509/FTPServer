package com.king.service;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.List;

public class MD5XmlParser {

    private final String currentDir;
    private final String absoluteFilename;
    private final String filename;

    public MD5XmlParser(String absoluteFilename) {
        this.absoluteFilename = absoluteFilename;
        File file = new File(absoluteFilename);
        currentDir = file.getParent();
        filename = file.getName();
    }



    /**
     * 从md5.xml文件中读取指定文件的md5值，若md5.xml不存在，则将计算所有文件的md5值，生成md5.xml文件
     * @return 目标文件md5值
     * @throws IOException
     * @throws DocumentException
     */
    public String readMD5(){
        try {
            createMD5XML();
            SAXReader reader = new SAXReader();
            System.out.println("Path: " + currentDir+"/md5.xml");
            InputStream in = new FileInputStream(new File(currentDir + "/md5.xml"));
            Document doc = reader.read(in);
            Element root = doc.getRootElement();
            List<Element> fileElems = root.elements();
            for (Element file : fileElems) {
                Attribute attribute = file.attribute(filename);
                if (attribute != null) {
                    System.out.println("md5: " + attribute.getValue());
                    return attribute.getValue();
                }
            }
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 创建md5.xml文件，保存当前工作目录下所有文件的md5值
     * @throws IOException
     */
    public void createMD5XML(){
        // 目标md5.xml文件
        File xmlFile = new File( currentDir + "/md5.xml");

        if(!xmlFile.exists()) {
            System.out.println("md5.xml doesn't exist.");
            System.out.println("starts to calculate md5 of all files");

            Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("files");

            /*计算当前目录下所有文件的md5值，并保存到md5.xml文件中*/
            File curDir = new File(currentDir);
            File[] files = curDir.listFiles();
            if(files != null)
                for(File file: files) {
                    if(file.isFile()) {
                        System.out.println(file.getName() + " starts to calculate");
                        String md5 = MD5.calMD5(file.getAbsolutePath());
                        root.addElement("file").addAttribute(file.getName(), md5);
                        System.out.println(file.getName() + " calculated. md5 is: " + md5);
                    }
                }

            // 自定义xml样式
            OutputFormat format = new OutputFormat();
            format.setIndentSize(2);  // 行缩进
            format.setNewlines(true); // 一个结点为一行
            format.setTrimText(true); // 去重空格
            format.setPadText(true);
            try {
                XMLWriter writer = new XMLWriter(new FileOutputStream(xmlFile), format);
                writer.write(doc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("md5 values calculated completely");
    }


    /**
     * 修改文件的md5值
      * @throws FileNotFoundException
     * @throws DocumentException
     */
    public void modifyMD5() throws FileNotFoundException, DocumentException {
        SAXReader reader = new SAXReader();
        InputStream in = new FileInputStream(new File(currentDir + "/md5.xml"));
        Document doc = reader.read(in);
        Element root = doc.getRootElement();
        List<Element> fileElems = root.elements();
        for(Element file: fileElems) {
            Attribute attribute = file.attribute(filename);
            if(attribute != null) {
                attribute.setValue(MD5.calMD5(filename));
                break;
            }
        }
    }
}
