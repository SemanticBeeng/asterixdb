package edu.uci.ics.hyracks.storage.am.lsmtree.freepage;

import static org.junit.Assert.fail;

import org.junit.Test;

import edu.uci.ics.hyracks.storage.am.lsmtree.common.impls.InMemoryBufferCache;
import edu.uci.ics.hyracks.storage.common.buffercache.HeapBufferAllocator;
import edu.uci.ics.hyracks.storage.common.buffercache.IBufferCache;
import edu.uci.ics.hyracks.storage.common.buffercache.IBufferCacheInternal;
import edu.uci.ics.hyracks.storage.common.buffercache.ICachedPage;

public class InMemoryBufferCacheTest{
	
    private static final int PAGE_SIZE = 256;
    private static final int NUM_PAGES = 10;
	
    //TEST InMemoryBufferCache.pin()
    @Test
    public void InMemoryBufferCacheTest01() throws Exception {
        IBufferCache memBufferCache = new InMemoryBufferCache(new HeapBufferAllocator(), PAGE_SIZE, NUM_PAGES);

        try {
        	ICachedPage memCachedPage = memBufferCache.pin(10, true);
        	if(memCachedPage != null) {
        		return;
        	}
        	else {
        		fail("fail to pin");
        	}
        }
        catch (ArrayIndexOutOfBoundsException e){
        	System.out.println("Catch exception: " + e);
        	return;
        }
        catch (Exception e) {
        	fail("Unexpected exception!");
        }
    }
    
    //TEST InMemoryBufferCache.pin()
	//expect ArrayIndexOutOfBoundsException
    @Test
    public void InMemoryBufferCacheTest02() throws Exception {
    	
        IBufferCache memBufferCache = new InMemoryBufferCache(new HeapBufferAllocator(), PAGE_SIZE, NUM_PAGES);

        try {
        	ICachedPage memCachedPage = memBufferCache.pin(500, true);
        	if(memCachedPage != null) {
        		return;
        	}
        	else {
        		fail("fail to pin");
        	}
        }
        catch (ArrayIndexOutOfBoundsException e){
        	System.out.println("Catch exception: " + e);
        	return;
        }
        catch (Exception e) {
        	fail("Unexpected exception!");
        }
    }
    
    //TEST InMemoryBufferCache.getPage()
    @Test
    public void InMemoryBufferCacheTest03() throws Exception {
    	
        IBufferCache memBufferCache = new InMemoryBufferCache(new HeapBufferAllocator(), PAGE_SIZE, NUM_PAGES);

        try {
        	ICachedPage memCachedPage = ((IBufferCacheInternal) memBufferCache).getPage(20);
        	if(memCachedPage != null) {
        		return;
        	}
        	else {
        		fail("fail to pin");
        	}
        }
        catch (ArrayIndexOutOfBoundsException e){
        	System.out.println("Catch exception: " + e);
        	return;
        }
        catch (Exception e) {
        	fail("Unexpected exception!");
        }
    } 
    
    //TEST InMemoryBufferCache.getPage()
    //expect ArrayIndexOutOfBoundsException
    @Test
    public void InMemoryBufferCacheTest04() throws Exception {
    	
        IBufferCache memBufferCache = new InMemoryBufferCache(new HeapBufferAllocator(), PAGE_SIZE, NUM_PAGES);

        try {
        	ICachedPage memCachedPage = ((IBufferCacheInternal) memBufferCache).getPage(1000);
        	if(memCachedPage != null) {
        		return;
        	}
        	else {
        		fail("fail to pin");
        	}
        }
        catch (ArrayIndexOutOfBoundsException e){
        	System.out.println("Catch exception: " + e);
        	return;
        }
        catch (Exception e) {
        	fail("Unexpected exception!");
        }
    } 
}
