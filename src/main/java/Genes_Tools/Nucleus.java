/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Genes_Tools;

/**
 *
 * @author phm
 */
public class Nucleus {
    
    // Cell index
    private int index;
    // Cell surface
    private double nucSurf;
    // Number of gene1 dots in cell
    private int gene1Dots;  
    // Gene1 dots surface
    private double gene1DotsSurf;
    // Number of gene2 dots in cell
    private int gene2Dots;  
    // Gene2 dots surface
    private double gene2DotsSurf;
   

   
	
	public Nucleus(int index, double nucSurf, int gene1Dots, double gene1DotsSurf, int gene2Dots, double gene2DotsSurf) {
            this.index = index;
            this.nucSurf = nucSurf;
            this.gene1Dots = gene1Dots;
            this.gene1DotsSurf = gene1DotsSurf;
            this.gene2Dots = gene2Dots;
            this.gene2DotsSurf = gene2DotsSurf;
	}
        
        public void setIndex(int index) {
            this.index = index;
	}
        
        public void setNucSurf(double nucSurf) {
            this.nucSurf = nucSurf;
	}
        
        public void setGene1Dots(int gene1Dots) {
            this.gene1Dots = gene1Dots;
	}
        
        public void gen1DotsSurf(double gene1DotsSurf) {
            this.gene1DotsSurf = gene1DotsSurf;
	}
        
        public void setGen2Dots(int gene2Dots) {
            this.gene2Dots = gene2Dots;
        }
        
        public void setgene2DotsSurf(double gene2DotsSurf) {
            this.gene2DotsSurf = gene2DotsSurf;
        }
        
        
        public int getIndex() {
            return index;
        }
        
        public double getNucSurfVol() {
            return nucSurf;
        }
                
        public int getGen1Dots() {
            return gene1Dots;
	}
        
        public double getGene1DotsSurf() {
            return gene1DotsSurf;
	}
        
        public int getGene2Dots() {
            return gene2Dots;
        }
        
        public double getGene2DotsSurf() {
            return gene2DotsSurf;
        }
                
}
