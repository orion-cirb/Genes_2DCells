package Genes_Tools;



import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.filter.RankFilters;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageLabeller;
import org.apache.commons.io.FilenameUtils;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import Orion_StardistPML.Orion_StarDist2D;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;



/**
 *
 * @author phm
 */

public class Genes_2DCells_Processing {
    
    public CLIJ2 clij2 = CLIJ2.getInstance();
    
    // min size for dots
    public double minDots = 0.05;
    // max size for dots
    public double maxDots = 5;
    // min size for nucleus
    public double minNuc = 5;
    // max size for nucleus
    public double maxNuc = 400;
    // nucleus parameters
    private double nucSigma1 = 30;
    private double nucSigma2 = 40;
    private String nucThMet = "Li";
    // genes parameters
    private double geneSigma1 = 4;
    private double geneSigma2 = 6;
    private String geneThMet = "Triangle";
    public Calibration cal = new Calibration();
    private double pixelSize = 0.13; 
    // use deep learning detection for nucleus on tile images (small)
    public boolean stardist = false;

    public final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    
     /**
     * check  installed modules
     * @return 
     */
    public boolean checkInstalledModules() {
        // check install
        ClassLoader loader = IJ.getClassLoader();
        try {
            loader.loadClass("net.haesleinhuepf.clij2.CLIJ2");
        } catch (ClassNotFoundException e) {
            IJ.log("CLIJ not installed, please install from update site");
            return false;
        }
        try {
            loader.loadClass("mcib3d.geom");
        } catch (ClassNotFoundException e) {
            IJ.log("3D ImageJ Suite not installed, please install from update site");
            return false;
        }
        return true;
    }
    
    
    
    /**
     * Dialog
     */
    public String dialog() {
        Boolean canceled = false;
        String[] methods = AutoThresholder.getMethods();
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets(0, 120, 0);
        gd.addImage(icon);
        gd.addDirectoryField("Images folder : ", "");
        gd.addMessage("Nucleus parameters", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("DOG radius 1 : ", nucSigma1, 1);
        gd.addNumericField("DOG radius 2 : ", nucSigma2, 1);
        gd.addChoice("Thresholding method :", methods, nucThMet);
        gd.addMessage("Genes parameters", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("DOG radius 1 : ", geneSigma1, 1);
        gd.addNumericField("DOG radius 2 : ", geneSigma2, 1);
        gd.addChoice("Thresholding method :", methods, geneThMet);
        gd.addMessage("--- Dots filter size ---", Font.getFont(Font.MONOSPACED), Color.blue);
        gd.addNumericField("Min. volume size : ", minDots, 3);
        gd.addNumericField("Max. volume size : ", maxDots, 3);
        gd.addMessage("Image calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Pixel size : ", pixelSize, 3);
        gd.addCheckbox("Use Deep Learning for nucleus detection", stardist);
        gd.showDialog();
        if (gd.wasCanceled())
            canceled = true;
        String imgFolder = gd.getNextString();
        nucSigma1 = gd.getNextNumber();
        nucSigma2 = gd.getNextNumber();
        nucThMet = gd.getNextChoice();
        geneSigma1 = gd.getNextNumber();
        geneSigma2 = gd.getNextNumber();
        geneThMet = gd.getNextChoice();
        minDots = gd.getNextNumber();
        maxDots = gd.getNextNumber();
        pixelSize = gd.getNextNumber();
        cal.pixelWidth = cal.pixelHeight = pixelSize;
        stardist = gd.getNextBoolean();
        return(imgFolder);
    } 
    
    
    
   /**
     * Find images in folder
     */
    public ArrayList findImages(String imagesFolder, String imageExt) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No Image found in "+imagesFolder);
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            // Find images with extension
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equalsIgnoreCase(imageExt))
                images.add(imagesFolder + File.separator + f);
        }
        Collections.sort(images);
        return(images);
    }
    
    /**
     * Find channels name
     * @param imageName
     * @return 
     * @throws loci.common.services.DependencyException
     * @throws loci.common.services.ServiceException
     * @throws loci.formats.FormatException
     * @throws java.io.IOException
     */
    public List<String> findChannels (String imageName) throws DependencyException, ServiceException, FormatException, IOException {
        List<String> channels = new ArrayList<>();
        // create OME-XML metadata store of the latest schema version
        ServiceFactory factory;
        factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata meta = service.createOMEXMLMetadata();
        ImageProcessorReader reader = new ImageProcessorReader();
        reader.setMetadataStore(meta);
        reader.setId(imageName);
        int chs = reader.getSizeC();
        String imageExt =  FilenameUtils.getExtension(imageName);
        switch (imageExt) {
            case "nd" :
                String channelsID = meta.getImageName(0);
                channels = Arrays.asList(channelsID.replace("_", "-").split("/"));
                break;
            case "lif" :
                String[] ch = new String[chs];
                if (chs > 1) {
                    for (int n = 0; n < chs; n++) 
                        if (meta.getChannelExcitationWavelength(0, n) == null)
                            channels.add(Integer.toString(n));
                        else 
                            channels.add(meta.getChannelExcitationWavelength(0, n).value().toString());
                }
                break;
            default :
                chs = reader.getSizeC();
                for (int n = 0; n < chs; n++)
                    channels.add(Integer.toString(n));
        }
        return(channels);         
    }
    
    
    /**
     *
     * @param img
     */
    public void closeImages(ImagePlus img) {
        img.flush();
        img.close();
    }

    
   /**
     * return objects population in an binary image
     * @param img
     * @return pop objects population
     */

    public  Objects3DPopulation getPopFromImage(ImagePlus img) {
        // label binary images first
        ImageLabeller labeller = new ImageLabeller();
        ImageInt labels = labeller.getLabels(ImageHandler.wrap(img));
        Objects3DPopulation pop = new Objects3DPopulation(labels);
        return pop;
    } 
    
    
    
    
    /**
     * Difference of Gaussians 
     * Using CLIJ2
     * @param imgCL
     * @param sizeX1
     * @param sizeY1
     * @param sizeX2
     * @param sizeY2

     * @return imgGauss
     */ 
    public ClearCLBuffer DOG(ClearCLBuffer imgCL, double sizeX1, double sizeY1, double sizeX2, double sizeY2) {
        ClearCLBuffer imgCLDOG = clij2.create(imgCL);
        clij2.differenceOfGaussian2D(imgCL, imgCLDOG, sizeX1, sizeY1, sizeX2, sizeY2);
        clij2.release(imgCL);
        return(imgCLDOG);
    }
    
    
    /**
     * Threshold 
     * USING CLIJ2
     * @param imgCL
     * @param thMed
     * @param fill 
     */
    public ClearCLBuffer threshold(ClearCLBuffer imgCL, String thMed) {
        ClearCLBuffer imgCLBin = clij2.create(imgCL);
        clij2.automaticThreshold(imgCL, imgCLBin, thMed);
        return(imgCLBin);
    }
    
    /*Median filter 
     * 
     * @param img
     * @param size
     */ 
    public void median_filter(ImagePlus img, double size) {
        RankFilters median = new RankFilters();
        for (int s = 1; s <= img.getNSlices(); s++) {
            img.setZ(s);
            median.rank(img.getProcessor(), size, RankFilters.MEDIAN);
            img.updateAndDraw();
        }
    }
    
    
    /**
     * return objects population in an binary image
     * Using CLIJ2
     * @param imgCL
     * @return pop
     */

    private Objects3DPopulation getPopFromClearBuffer(ClearCLBuffer imgCL) {
        ClearCLBuffer output = clij2.create(imgCL);
        clij2.connectedComponentsLabelingBox(imgCL, output);
        ImagePlus imgLab  = clij2.pull(output);
        clij2.release(output);
        //imgLab.show();
        //new WaitForUserDialog(nucThMet).show();
        Objects3DPopulation pop = new Objects3DPopulation(imgLab);
        pop.setCalibration(cal.pixelWidth, cal.pixelDepth,cal.getUnits());
        return pop;
    }  

    /**
     * Find dots population
     * @param imgGene
     * @return dotsPop
     */
    public Objects3DPopulation findGenePop(ImagePlus imgGene) {
        IJ.showStatus("Finding gene dots ...");
        median_filter(imgGene, 1);
        ClearCLBuffer imgCLMed = clij2.push(imgGene);
        ClearCLBuffer imgCLDOG = DOG(imgCLMed, geneSigma1, geneSigma1, geneSigma2, geneSigma2);
        clij2.release(imgCLMed);
        ClearCLBuffer imgCLBin = threshold(imgCLDOG, geneThMet); 
        clij2.release(imgCLDOG);
        Objects3DPopulation dotsPop = new Objects3DPopulation(getPopFromClearBuffer(imgCLBin).getObjectsWithinVolume(minDots, maxDots, true));
        clij2.release(imgCLBin);       
        return(dotsPop);
    }
    
    /**
     * Nucleus segmentation
     * @param imgNuc
     * @return 
     */
    public Objects3DPopulation findNucleus(ImagePlus imgNuc) {
        ImagePlus img = new Duplicator().run(imgNuc);
        img.setCalibration(cal);
        IJ.run(img, "Remove Outliers", "block_radius_x=40 block_radius_y=40 standard_deviations=1");
        IJ.run(img, "Nuclei Outline", "blur="+nucSigma1+" blur2="+nucSigma2+" threshold_method="+nucThMet+" outlier_radius=0 outlier_threshold=1 max_nucleus_size=200 "
                    + "min_nucleus_size=20 erosion=1 expansion_inner=1 expansion=1 results_overlay");
        ImagePlus mask = new ImagePlus("mask", img.createRoiMask().getBufferedImage());
        ImageProcessor ip =  mask.getProcessor();
        ip.invertLut();
        mask.setCalibration(cal);
        Objects3DPopulation cellPop = new Objects3DPopulation(getPopFromImage(mask).getObjectsWithinVolume​(minNuc, maxNuc, true));
        closeImages(img);
        closeImages(mask);
        return(cellPop);
    }
    
    
    /** Look for all nuclei
     * work on images <= 2048
     * split image if size > 2048
     * return nuclei population
     */
    public Objects3DPopulation stardistNucleiPop(ImagePlus imgNuc){
        ImagePlus img = new Duplicator().run(imgNuc);
        IJ.run(img, "Remove Outliers", "block_radius_x=40 block_radius_y=40 standard_deviations=1");
        Orion_StarDist2D star = new Orion_StarDist2D();
        star.checkImgSize(img);
        star.loadInput(img);
        star.run();
        Img<? extends RealType<?>> img1 = star.label.getImgPlus().getImg();
        ImagePlus imgLab = ImageJFunctions.wrap((RandomAccessibleInterval)img1, "Labelled");
        imgLab.setCalibration(cal);
        Objects3DPopulation nucPop = new Objects3DPopulation(imgLab);
        ArrayList<Object3D> objectsWithinVolume = nucPop.getObjectsWithinVolume(minNuc, maxNuc, true);
        Objects3DPopulation popFilter = new Objects3DPopulation(objectsWithinVolume);
        closeImages(imgLab);
        return(popFilter);
    }
    
   
    
    /**
     * Find dots population in outlined cells
     * @param nucObj
     * @param dotsPop
     * @return dots
     */    
    private Objects3DPopulation findDotsInNucleus(Object3D nucObj, Objects3DPopulation dotsPop) {
        Objects3DPopulation dots = new Objects3DPopulation();
        for (int n = 0; n < dotsPop.getNbObjects(); n++) {
            Object3D dotObj = dotsPop.getObject(n);
            if (dotObj.hasOneVoxelColoc(nucObj))
                dots.addObject(dotObj);
        }
        return(dots);
    }
    
    
    
    private double geneSurf(Objects3DPopulation genePop) {
        double dotSurf = 0;
        for (int n = 0; n < genePop.getNbObjects(); n++) {
            Object3D dotObj = genePop.getObject(n);
            dotSurf += dotObj.getAreaUnit();
        }
        return(dotSurf);
    }
    
    
    /**
     * Tags cell with genes dots
     * @param imgDapi
     * @param nucPop
     * @param gene1Pop
     * @param gene2Pop
     * @return 
     */
    
    public ArrayList<Nucleus> tagsCells(ImagePlus imgDapi, Objects3DPopulation nucPop, Objects3DPopulation gene1Pop, Objects3DPopulation gene2Pop) {
        
        ArrayList<Nucleus> nucleusList = new ArrayList<>();
        IJ.showStatus("Finding nucleus parameters ...");
        ImageHandler imhPML = ImageHandler.wrap(imgDapi);
        int index = 0;
        
        for (int i = 0; i < nucPop.getNbObjects(); i++) {
            // calculate cell parameters
            index++;
            Object3D nucObj = nucPop.getObject(i);
            double nucSurf = nucObj.getAreaUnit();
            
            // dots parameters
            Objects3DPopulation gene1Nuc = findDotsInNucleus(nucObj, gene1Pop);
            Objects3DPopulation gene2Nuc = findDotsInNucleus(nucObj, gene2Pop);
            double  dots1Surf = geneSurf(gene1Nuc);
            double  dots2Surf = geneSurf(gene2Nuc);
            
            Nucleus nucleus = new Nucleus(index, nucSurf, gene1Nuc.getNbObjects(), dots1Surf, gene2Nuc.getNbObjects(), dots2Surf);
            nucleusList.add(nucleus);
        }
        return(nucleusList);
    }
    

    /**
     * Label object
     * @param popObj
     * @param img 
     */
    public void labelsObject (Objects3DPopulation popObj, ImagePlus img, int fontSize) {
        Font tagFont = new Font("SansSerif", Font.PLAIN, fontSize);
        String name;
        for (int n = 0; n < popObj.getNbObjects(); n++) {
            Object3D obj = popObj.getObject(n);
            name = Integer.toString(n+1);
            int[] box = obj.getBoundingBox();
            int z = (int)obj.getCenterZ();
            int x = box[0] - 1;
            int y = box[2] - 1;
            img.setSlice(z+1);
            ImageProcessor ip = img.getProcessor();
            ip.setFont(tagFont);
            ip.setColor(255);
            ip.drawString(name, x, y);
            img.updateAndDraw();
        }
    }
    
   
    
    
   
}
