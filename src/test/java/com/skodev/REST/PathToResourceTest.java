/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.skodev.REST;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author HOME
 */
public class PathToResourceTest {
    
    public PathToResourceTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @org.junit.Test
    public void testOkNoEndSlashes() {
        String[] expectedArr = {"one", "two", "three"};
        PathToResource ptr = new PathToResource("one/two/three");
        String[] arr = ptr.getDirList();
        assertArrayEquals(expectedArr, arr);
    }
    
    @org.junit.Test
    public void testOkRightSlash() {
        String[] expectedArr = {"one", "two", "three"};
        PathToResource ptr = new PathToResource("one/two/three/");
        String[] arr = ptr.getDirList();
        assertArrayEquals(expectedArr, arr);
    }
    
    @org.junit.Test
    public void testOkNoLeftSlash() {
        String[] expectedArr = {"one", "two", "three"};
        PathToResource ptr = new PathToResource("/one/two/three");
        String[] arr = ptr.getDirList();
        assertArrayEquals(expectedArr, arr);
    }
    
    @org.junit.Test
    public void testOkEndSlashes() {
        String[] expectedArr = {"one", "two", "three"};
        PathToResource ptr = new PathToResource("/one/two/three/");
        String[] arr = ptr.getDirList();
        assertArrayEquals(expectedArr, arr);
    }
    
    @org.junit.Test
    public void testEmpty() {
        String[] expectedArr = {""};
        PathToResource ptr = new PathToResource("/");
        String[] arr = ptr.getDirList();
        assertArrayEquals(expectedArr, arr);
    }
    
}
