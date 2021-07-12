/*
 * Find dots in outlined Cells 
 * Measure integrated intensity, nb of dots per cells detect if cell infected 
 * Author Philippe Mailly
 */

import Genes_Tools.Nucleus;
import ij.*;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageHandler;
import java.util.ArrayList;
import org.apache.commons.io.FilenameUtils;


public class Genes_2DCells implements PlugIn {

    private final boolean canceled = false;
    private String imageDir = "";
    public String outDirResults = "";
    private BufferedWriter outPutResults;
    
    private Genes_Tools.Genes_2DCells_Processing genes = new Genes_Tools.Genes_2DCells_Processing();

      

    /**
     * 
     * @param arg
     */
    @Override
    public void run(String arg) {
                
        if (canceled) {
            IJ.showMessage(" Pluging canceled");
            return;
        }
        imageDir = genes.dialog();
        if (imageDir == null) {
            return;
        }
        String fileExt = "tif";
        File inDir = new File(imageDir);
        ArrayList<String> imageFiles = genes.findImages(imageDir, fileExt);
        if (imageFiles == null) {
            return;
        }
        // create output folder
        outDirResults = inDir + File.separator+ "Results"+ File.separator;
        File outDir = new File(outDirResults);
        if (!Files.exists(Paths.get(outDirResults))) {
            outDir.mkdir();
        }

        // Write headers results for results file
        FileWriter fileResults = null;
        String resultsName = "results.xls";
        try {
            fileResults = new FileWriter(outDirResults + resultsName, false);
        } catch (IOException ex) {
            Logger.getLogger(Genes_2DCells.class.getName()).log(Level.SEVERE, null, ex);
        }
        outPutResults = new BufferedWriter(fileResults);
        try {
            outPutResults.write("ImageName\t#Nucleus\tNucleus surface (µm2)\tGene1 dot number\tGene1 surface (µm2)\tGene2 dot number\tGene2 surface (µm2)\n");
            outPutResults.flush();
        } catch (IOException ex) {
            Logger.getLogger(Genes_2DCells.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Read images
        for (int f = 0; f < imageFiles.size(); f += 3) {
            String rootName = FilenameUtils.getBaseName(imageFiles.get(f)).replace("C1", "");
            // find dapi and genes channel images

            // Open DAPI Channel and detect nucleus
            ImagePlus imgDapi = IJ.openImage(imageFiles.get(f));
            Objects3DPopulation nucPop = new Objects3DPopulation();
            if (genes.stardist)
                nucPop = genes.stardistNucleiPop(imgDapi);
            else
                nucPop = genes.findNucleus(imgDapi);
            System.out.println("Total nucleus found = "+nucPop.getNbObjects());

            // Open Gene1 image
            String gene1 = imageFiles.get(f+1).replace("C1", "C2");
            System.out.println(gene1);
            ImagePlus imgGene1 = IJ.openImage(gene1);
            Objects3DPopulation gene1Pop = genes.findGenePop(imgGene1);
            System.out.println("Total gene1 dots = "+gene1Pop.getNbObjects());

            // Open Gene2 image
            String gene2 = imageFiles.get(f+2).replace("C1", "C3");
            ImagePlus imgGene2 = IJ.openImage(gene2);
            Objects3DPopulation gene2Pop = genes.findGenePop(imgGene2);
            System.out.println("Total gene2 dots = "+gene2Pop.getNbObjects());


            // For all nucleus find gene dots
            ArrayList<Nucleus> nucleus = genes.tagsCells(imgDapi, nucPop, gene1Pop, gene2Pop);

                // Write parameters                        
                IJ.showStatus("Writing parameters ...");
                for (Nucleus nuc : nucleus) {
                try {
                    outPutResults.write(rootName+"\t"+nuc.getIndex()+"\t"+nuc.getNucSurfVol()+"\t"+nuc.getGen1Dots()+"\t"+nuc.getGene1DotsSurf()+"\t"
                            +nuc.getGene2Dots()+"\t"+nuc.getGene2DotsSurf()+"\n");
                    outPutResults.flush();
                } catch (IOException ex) {
                    Logger.getLogger(Genes_2DCells.class.getName()).log(Level.SEVERE, null, ex);
                }
                }
                // Save objects image
                ImageHandler imhNuc = ImageHandler.wrap(imgDapi).createSameDimensions();
                ImageHandler imhGene1 = imhNuc.createSameDimensions();
                ImageHandler imhGene2 = imhNuc.createSameDimensions();
                // draw nucleus
                genes.labelsObject(nucPop, imhNuc.getImagePlus(), 24);
                 nucPop.draw(imhNuc, 255);
                // draw genes
                gene1Pop.draw(imhGene1, 255);
                gene2Pop.draw(imhGene2, 255);

                ImagePlus[] imgColors = {imhGene1.getImagePlus(), imhGene2.getImagePlus(), imhNuc.getImagePlus()};
                ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
                imgObjects.setCalibration(imgDapi.getCalibration());
                FileSaver ImgObjectsFile = new FileSaver(imgObjects);
                ImgObjectsFile.saveAsTiff(outDirResults+rootName+"_Objects.tif"); 
                genes.closeImages(imgDapi);
                genes.closeImages(imgGene1);
                genes.closeImages(imgGene2);
            }
        IJ.showStatus("Process done");
       
    }
}