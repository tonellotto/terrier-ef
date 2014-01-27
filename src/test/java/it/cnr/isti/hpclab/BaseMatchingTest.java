package it.cnr.isti.hpclab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;

public class BaseMatchingTest 
{
	protected static final String weightingModel = "BM25";
	
	protected static final String indexPath = "/Users/khast/index-java/";
	protected static final String indexPrefix = "wt10g";
	// protected static final String query = "Jackie Robinson appear at his first game"; // for wt10g
	protected static final String query = "attori"; // for wt10g

	protected static int[] correctDocids = null;
	protected static double[] correctScores = null;
	protected static Index index = null;
	
	
	@BeforeClass 
	public static void runOriginalMatching() throws IOException
	{
		ApplicationSetup.setProperty("stopwords.filename", System.getProperty("user.dir") + File.separator + "src/main/resources/stopword-list.txt");
		ApplicationSetup.setProperty("terrier.properties", System.getProperty("user.dir") + File.separator + "src/main/resources/terrier.properties");

		setIndex(indexPath, indexPrefix);

		Manager m = new Manager(index);
		
		SearchRequest srq = m.newSearchRequest("1", query);
		srq.addMatchingModel("org.terrier.matching.OldBasicMatching", weightingModel);
		
		m.runPreProcessing(srq);
		m.runMatching(srq);
		m.runPostProcessing(srq);
		m.runPostFilters(srq);

		final ResultSet result = srq.getResultSet();
		correctDocids = result.getDocids();
		correctScores = result.getScores();
		
		System.out.println("Found " + result.getResultSize() + " results in original matching");
	}
	
	@AfterClass 
	public static void close() throws IOException 
	{
		index.close();
	}
	
	@Test
	public void runUnmodifiedMatching() throws IOException
	{
		BaseMatchingTest.setIndex(indexPath, indexPrefix);
		this.runMatching("org.terrier.matching.OldBasicMatching");
	}
	
	protected static void setIndex(String path, String prefix) throws IOException
	{
		if (index != null)
			index.close();
		
		index = Index.createIndex(path, prefix);
		if (Index.getLastIndexLoadError() != null) 
			System.err.println(Index.getLastIndexLoadError());
	}
	
	protected void runMatching(String matchingClass) throws IOException
	{
		Manager m = new Manager(index);
		
		SearchRequest srq = m.newSearchRequest("1", query);
		srq.addMatchingModel(matchingClass, weightingModel);
		
		m.runPreProcessing(srq);
		m.runMatching(srq);
		m.runPostProcessing(srq);
		m.runPostFilters(srq);

		final ResultSet result = srq.getResultSet();
		int[] docids = result.getDocids();
		double[] scores = result.getScores();
		
		System.out.println("Found " + result.getResultSize() + " results in " + matchingClass + " matching");

		/*
		for (int i = 0; i < docids.length; i++) {
			System.err.println(correctDocids[i] + " \t" + docids[i]);
			System.err.println(correctScores[i] + " \t" + scores[i]);
		}
		*/
		
		Set<Integer> correctSubset = new HashSet<Integer>();
		Set<Integer> currentSubset = new HashSet<Integer>();
		
		for (int i = 0; i < correctDocids.length; i++) {
			// The docids with the same score can have different order in both lists
			if (i != docids.length - 1 && scores[i] == scores[i+1]) {
				correctSubset.add(correctDocids[i]);
				currentSubset.add(docids[i]);
			} else if (i == docids.length - 1) {
				// The last subsets might not contain correct docids if the result set has been resized
				break;
			} else if (currentSubset.isEmpty()) {
				assertEquals(docids[i], correctDocids[i]);
			} else {
				correctSubset.add(correctDocids[i]);
				currentSubset.add(docids[i]);
				for (int id : correctSubset)
					assertTrue(currentSubset.contains(id));
				
				correctSubset.clear();
				currentSubset.clear();
			}
			// The arrays are score-sorted so no problems on scores
			assertEquals(scores[i], correctScores[i], 0.0000001);
		}
		
	}
}