import javax.swing.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class MyImageDisplay {
    //显示图片的长和宽
    private static int imgWith = 352;
    private static int imgHeight = 288;

    //初始用于rgb和yuv数据的数组
    private static int imageCopy[] = new int[3* imgHeight * imgWith];
    private static double yuvImage[] = new double[3* imgHeight * imgWith];
    public static void main(String[] args)throws Exception {
        //输入进程序的五个参数
        String filePath = args[0];
        int Y = Integer.parseInt(args[1]);
        int U = Integer.parseInt(args[2]);
        int V = Integer.parseInt(args[3]);
        int Q = Integer.parseInt(args[4]);

        BufferedImage imgOriginal = getOriginalImg(filePath);
        //进行子采样
        subSample(Y,U,V);
        //获取处理后的图像
        BufferedImage img = processImg(Q);
        //显示图片至面板
        displayImg(imgOriginal,img);
    }
    //获取原始图像
    public static BufferedImage getOriginalImg(String fileName)throws Exception{
        BufferedImage imgOriginal = new BufferedImage(imgWith, imgHeight, BufferedImage.TYPE_INT_RGB);
        InputStream is = new FileInputStream(new File(fileName));
        byte[] size = new byte[(int)new File(fileName).length()];
        //计算偏移量
        int offset = 0;
        int numRead = 0;
        while (offset < size.length && (numRead=is.read(size, offset, size.length-offset)) >= 0) {
            offset =offset+ numRead;
        }
        calculationRgbAndYuv( size,imgOriginal);
        return imgOriginal;
    }

    //处理输出图像
    public static BufferedImage processImg(int Q){
        BufferedImage img = new BufferedImage(imgWith, imgHeight, BufferedImage.TYPE_INT_RGB);
        int y=0;
        while(y < imgHeight){
            int x = 0;
            while (x < imgWith){
                int i = y* imgWith + x;
                byte r, g, b;
                //从yuv值计算rgb值
                //打转
                //然后做量子化
                //然后消除小于0或大于255的数据
                imageCopy[3*i] = (int)Math.rint(yumChangeUtil("R",yuvImage[3*i], yuvImage[3*i+1], yuvImage[3*i+2]));
                imageCopy[3*i] = quantization(imageCopy[3*i],Q);
                if(imageCopy[3*i] < 0) {
                    imageCopy[3*i] = 0;
                } else if(imageCopy[3*i] > 255) {
                    imageCopy[3*i] = 255;
                }

                imageCopy[3*i +1] = (int)Math.rint(yumChangeUtil("G",yuvImage[3*i], yuvImage[3*i+1], yuvImage[3*i+2]));
                imageCopy[3*i + 1] = quantization(imageCopy[3*i +1],Q);
                if(imageCopy[3*i + 1] < 0) {
                    imageCopy[3*i + 1] = 0;
                } else if(imageCopy[3*i + 1] > 255) {
                    imageCopy[3*i + 1] = 255;
                }

                imageCopy[3*i +2] = (int)Math.rint(yumChangeUtil("B",yuvImage[3*i], yuvImage[3*i+1], yuvImage[3*i+2]));
                imageCopy[3*i + 2] = quantization(imageCopy[3*i + 2],Q);
                if(imageCopy[3*i + 2] < 0) {
                    imageCopy[3*i + 2] = 0;
                } else if(imageCopy[3*i + 2] > 255) {
                    imageCopy[3*i + 2] = 255;
                }

                //note here we need transform int data 0-255
                //注意这里我们需要转换int数据0-255
                //to byte data which is -128 to 127
                //到字节数据-128到127

                if(imageCopy[3*i] > 127) {
                    r = (byte)(imageCopy[3*i] - 256);
                } else {
                    r = (byte)imageCopy[3*i];
                }

                if(imageCopy[3*i + 1] > 127) {
                    g = (byte)(imageCopy[3*i + 1] - 256);
                } else {
                    g = (byte)imageCopy[3*i + 1];
                }

                if(imageCopy[3*i + 2] > 127) {
                    b = (byte)(imageCopy[3*i + 2] - 256);
                } else {
                    b = (byte)imageCopy[3*i + 2];
                }

                //设置显示输出图像的像素
                int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                img.setRGB(x,y,pix);
                x++;
            }
            y++;
        }
        return img;
    }
    //显示图像
    public static void displayImg(BufferedImage imgOriginal,BufferedImage img){
        JPanel  panel = new JPanel ();
        panel.add (new JLabel (new ImageIcon (imgOriginal)));
        panel.add (new JLabel (new ImageIcon (img)));
        JFrame frame = new JFrame();
        frame.getContentPane().add (panel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    //计算rgb和yuv并分别存储进全局变量中
    public static void  calculationRgbAndYuv(byte[] size,BufferedImage imgOriginal){
        int ind = 0;
        int y = 0;
        while ( y < imgHeight){
            int x = 0;
            while (  x < imgWith){
                byte a = 0;
                //从原始图像读取r值并将其存储在imageCopy[]
                //注意这里Java中的字节值是从-128到127
                //我们需要将其转换为0-255，以便进一步进行yuv计算
                byte r = size[ind];
                if(r<0) {
                    imageCopy[(x + y* imgWith)*3] = r+256;
                } else {
                    imageCopy[(x + y* imgWith)*3] = r;
                }

                byte b = size[ind+ imgHeight * imgWith *2];
                if(b<0) {
                    imageCopy[(x + y* imgWith)*3 + 2] = b+256;
                } else {
                    imageCopy[(x + y* imgWith)*3 + 2] = b;
                }

                byte g = size[ind+ imgHeight * imgWith];
                if(g<0) {
                    imageCopy[(x + y* imgWith)*3 + 1] = g+256;
                } else {
                    imageCopy[(x + y* imgWith)*3 + 1] = g;
                }
                //计算yuv值并将其存储在yuvImage[]
                calculationYuv(x,y);
                //设置显示原始图像的像素
                int pixOriginal = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                imgOriginal.setRGB(x,y,pixOriginal);
                ind++;
                x++;
            }
            y++;
        }
    }
    //计算yuv值
    public static void calculationYuv(int x,int y){
        //计算yuv值并将其存储在yuvImage[]
        double changedU = rgbChangeUtil("U",imageCopy[(x +y* imgWith )*3], imageCopy[(x + y* imgWith)*3 + 1], imageCopy[(y* imgWith + x)*3 + 2]);
        yuvImage[(x +y* imgWith )*3+1] = changedU;

        double changedV = rgbChangeUtil("V",imageCopy[(x +y* imgWith )*3], imageCopy[(x + y* imgWith)*3 + 1], imageCopy[(y* imgWith + x)*3 + 2]);
        yuvImage[(x +y* imgWith )*3+2] = changedV;

        double changedY = rgbChangeUtil("Y",imageCopy[(x + y* imgWith )*3], imageCopy[(x + y* imgWith)*3 + 1], imageCopy[(y* imgWith + x)*3 + 2]);
        yuvImage[(x +y* imgWith )*3] = changedY;
    }
    //子采样方法
    public static void subSample(int Y,int U,int V) {
        int y = 0;
        while( y < imgHeight){
            if(Y > 1) {
                int x=0;
                while (x < imgWith - Y){
                    //将x到x+Y之间的值设置为两个值的平均值
                    //剩余价值
                    int z=x+1;
                    while( z<x+Y) {
                        yuvImage[3*(imgWith *y + z)] = (yuvImage[3*(imgWith *y + x)] + yuvImage[3*(imgWith *y + x+Y)])/2;
                        z++;
                    }
                    x+=Y;
                }
                x++;
                //注意这里的最后一段值
                //我们将它们设置为起始值
                while (x< imgWith) {
                    yuvImage[3*(imgWith *y + x)] = yuvImage[3*(imgWith *y + imgWith - Y)];
                    x++;
                }
            }

            if(U > 1) {
                int x=0;
                while ( x < imgWith - U){
                    int z=x+1;
                    while (z<x+U) {
                        yuvImage[3*(imgWith *y + z) + 1] = (yuvImage[3*(imgWith *y + x) + 1] + yuvImage[3*(imgWith *y + x+U) + 1])/2;
                        z++;
                    }
                    x+=U;
                }
                x++;
                while (x< imgWith) {
                    yuvImage[3*(imgWith *y + x) + 1] = yuvImage[3*(imgWith *y + imgWith - U) + 1];
                    x++;
                }
            }

            if(Y > 1) {
                int x=0;
                while ( x < imgWith - V){
                    int z=x+1;
                    while ( z<x+V) {
                        yuvImage[3*(imgWith *y + z) + 2] = (yuvImage[3*(imgWith *y + x) + 2] + yuvImage[3*(imgWith *y + x+V) + 2])/2;
                        z++;
                    }
                    x+=V;
                }
                x++;
                while ( x< imgWith) {
                    yuvImage[3*(imgWith *y + x) + 2] = yuvImage[3*(imgWith *y + imgWith - V) + 2];
                    x++;
                }
            }
            y++;
        }
    }

    //做量子化
    //注意这里我们需要设置最后一级的所有输入
    //为最后一级开始的次数
    public static int quantization(int input,int Q) {
        if(Q < 256 && Q > 0) {
            int span = (256/Q);
            if(input > (255-span)) {
                return (255-span);
            }
            int level = (int)Math.rint((input+1.0)/span);
            return span*level-1;
        }
        return input;
    }
    //rgb值计算工具
    public static double rgbChangeUtil(String toType,int r, int g, int b){
        if (toType.equals("Y")){//返回Y值
            return (0.299*r + 0.587*g + 0.114*b);
        }else if (toType.equals("U")){//返回U值
            return (0.436*b - (0.147*r) - (0.289*g));
        }else if (toType.equals("V")){//返回V值
            return (0.615*r - (0.515*g) - (0.100*b));
        }else {
            return 0;
        }
    }
    //yuv值计算工具
    public static double yumChangeUtil(String toType,double y, double u, double v){
        if (toType.equals("R")){//返回R值
            return (0.999*y + 1.140*v);
        }else if (toType.equals("G")){//返回G值
            return (y - 0.395*u - 0.581*v);
        }else if (toType.equals("B")){//返回B值
            return (y + 2.032*u);
        }else {
            return 0;
        }
    }
}
