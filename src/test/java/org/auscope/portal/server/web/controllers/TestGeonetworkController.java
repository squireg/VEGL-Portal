package org.auscope.portal.server.web.controllers;

import org.auscope.portal.csw.record.CSWRecord;
import org.auscope.portal.server.cloud.S3FileInformation;
import org.auscope.portal.server.vegl.VEGLJob;
import org.auscope.portal.server.vegl.VEGLJobManager;
import org.auscope.portal.server.vegl.VEGLSeries;
import org.auscope.portal.server.web.service.GeonetworkService;
import org.auscope.portal.server.web.service.JobStorageService;
import org.jets3t.service.S3ServiceException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

/**
 * Units tests for GeonetworkController
 * @author Josh Vote
 *
 */
public class TestGeonetworkController {
    private Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    private VEGLJobManager mockJobManager;
    private GeonetworkService mockGNService;
    private JobStorageService mockJobStorageService;

    private GeonetworkController controller;

    @Before
    public void init() {
        mockJobManager = context.mock(VEGLJobManager.class);
        mockGNService = context.mock(GeonetworkService.class);
        mockJobStorageService = context.mock(JobStorageService.class);

        controller = new GeonetworkController(mockJobManager, mockGNService, mockJobStorageService);
    }

    /**
     * Tests that the insertRecord function correctly uses all dependencies on success.
     * @throws Exception
     */
    @Test
    public void testInsertRecord() throws Exception {
        final Integer jobId = 1235;
        final Integer seriesId = 5432;
        final VEGLJob mockJob = context.mock(VEGLJob.class);
        final VEGLSeries mockSeries = context.mock(VEGLSeries.class);
        final String registeredUrl  = "http://example.csw.url/";
        final S3FileInformation[] outputFileInfo = new S3FileInformation[] {
                new S3FileInformation("my/key1", 100L, "http://public.url1"),
                new S3FileInformation("my/key2", 200L, "http://public.url2"),
                new S3FileInformation("my/key3", 300L, "http://public.url3")
        };

        //We want to ensure our job is set values BEFORE saving it
        final Sequence jobSavingSequence = context.sequence("jobSavingSequence");

        context.checking(new Expectations() {{
            //Our mock job configuration
            allowing(mockJob).getSelectionMaxEasting();will(returnValue(1.0));
            allowing(mockJob).getSelectionMinEasting();will(returnValue(2.0));
            allowing(mockJob).getSelectionMaxNorthing();will(returnValue(3.0));
            allowing(mockJob).getSelectionMinNorthing();will(returnValue(4.0));
            allowing(mockJob).getDescription();will(returnValue("description"));
            allowing(mockJob).getSubmitDate();will(returnValue("20110713_105730"));
            allowing(mockJob).getName();will(returnValue("name"));
            allowing(mockJob).getUser();will(returnValue("user"));
            allowing(mockJob).getSeriesId();will(returnValue(seriesId));
            allowing(mockJob).getS3OutputBucket();will(returnValue("s3-output-bucket"));
            allowing(mockJob).getEmailAddress();will(returnValue("email@address"));

            //Our series configuration
            allowing(mockSeries).getName();will(returnValue("seriesName"));
            allowing(mockSeries).getDescription();will(returnValue("seriesDescription"));

            //We should make a single call to the database for job objects
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(mockJob));
            oneOf(mockJobManager).getSeriesById(seriesId);will(returnValue(mockSeries));

            //Only 1 call to the job storage service for files
            oneOf(mockJobStorageService).getOutputFileDetails(mockJob);will(returnValue(outputFileInfo));

            //Only 1 call to save our newly created record
            oneOf(mockGNService).makeCSWRecordInsertion(with(any(CSWRecord.class)));will(returnValue(registeredUrl));

            //This must occur in sequence (set our value before saving it)
            oneOf(mockJob).setRegisteredUrl(registeredUrl);inSequence(jobSavingSequence);
            oneOf(mockJobManager).saveJob(mockJob);inSequence(jobSavingSequence);
        }});

        ModelAndView mav = controller.insertRecord(jobId);
        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean) mav.getModel().get("success"));
        Assert.assertEquals(registeredUrl, mav.getModel().get("data"));
    }

    /**
     * Tests that the insertRecord function correctly fails when the job object DNE.
     * @throws Exception
     */
    @Test
    public void testInsertRecordJobDNE() throws Exception {
        final Integer jobId = 1235;

        context.checking(new Expectations() {{

            //We should make a single call to the database for job objects
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(null));
        }});

        ModelAndView mav = controller.insertRecord(jobId);
        Assert.assertNotNull(mav);
        Assert.assertFalse((Boolean) mav.getModel().get("success"));
    }

    /**
     * Tests that the insertRecord function correctly fails when the job series DNE
     * @throws Exception
     */
    @Test
    public void testInsertRecordSeriesDNE() throws Exception {
        final Integer jobId = 1235;
        final Integer seriesId = 5432;
        final VEGLJob mockJob = context.mock(VEGLJob.class);

        context.checking(new Expectations() {{
            //Our mock job configuration
            allowing(mockJob).getSeriesId();will(returnValue(seriesId));

            //We should make a single call to the database for job objects
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(mockJob));
            oneOf(mockJobManager).getSeriesById(seriesId);will(returnValue(null));
        }});

        ModelAndView mav = controller.insertRecord(jobId);
        Assert.assertNotNull(mav);
        Assert.assertFalse((Boolean) mav.getModel().get("success"));
    }

    /**
     * Tests that the insertRecord function correctly fails when there is a failure to lookup S3 output files.
     * @throws Exception
     */
    @Test
    public void testInsertRecordS3Failure() throws Exception {
        final Integer jobId = 1235;
        final Integer seriesId = 5432;
        final VEGLJob mockJob = context.mock(VEGLJob.class);
        final VEGLSeries mockSeries = context.mock(VEGLSeries.class);

        context.checking(new Expectations() {{
            //Our mock job configuration
            allowing(mockJob).getSeriesId();will(returnValue(seriesId));

            //We should make a single call to the database for job objects
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(mockJob));
            oneOf(mockJobManager).getSeriesById(seriesId);will(returnValue(mockSeries));

            //Only 1 call to the job storage service for files
            oneOf(mockJobStorageService).getOutputFileDetails(mockJob);will(throwException(new S3ServiceException()));
        }});

        ModelAndView mav = controller.insertRecord(jobId);
        Assert.assertNotNull(mav);
        Assert.assertFalse((Boolean) mav.getModel().get("success"));
    }

    /**
     * Tests that the insertRecord function correctly fails when registration to geonetwork fails
     * @throws Exception
     */
    @Test
    public void testInsertRecordGNServiceError() throws Exception {
        final Integer jobId = 1235;
        final Integer seriesId = 5432;
        final VEGLJob mockJob = context.mock(VEGLJob.class);
        final VEGLSeries mockSeries = context.mock(VEGLSeries.class);
        final S3FileInformation[] outputFileInfo = new S3FileInformation[] {
                new S3FileInformation("my/key1", 100L, "http://public.url1"),
                new S3FileInformation("my/key2", 200L, "http://public.url2"),
                new S3FileInformation("my/key3", 300L, "http://public.url3")
        };

        context.checking(new Expectations() {{
            //Our mock job configuration
            allowing(mockJob).getSelectionMaxEasting();will(returnValue(1.0));
            allowing(mockJob).getSelectionMinEasting();will(returnValue(2.0));
            allowing(mockJob).getSelectionMaxNorthing();will(returnValue(3.0));
            allowing(mockJob).getSelectionMinNorthing();will(returnValue(4.0));
            allowing(mockJob).getDescription();will(returnValue("description"));
            allowing(mockJob).getSubmitDate();will(returnValue("20110713_105730"));
            allowing(mockJob).getName();will(returnValue("name"));
            allowing(mockJob).getUser();will(returnValue("user"));
            allowing(mockJob).getSeriesId();will(returnValue(seriesId));
            allowing(mockJob).getS3OutputBucket();will(returnValue("s3-output-bucket"));
            allowing(mockJob).getEmailAddress();will(returnValue("email@address"));

            //Our series configuration
            allowing(mockSeries).getName();will(returnValue("seriesName"));
            allowing(mockSeries).getDescription();will(returnValue("seriesDescription"));

            //We should make a single call to the database for job objects
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(mockJob));
            oneOf(mockJobManager).getSeriesById(seriesId);will(returnValue(mockSeries));

            //Only 1 call to the job storage service for files
            oneOf(mockJobStorageService).getOutputFileDetails(mockJob);will(returnValue(outputFileInfo));

            //Only 1 call to save our newly created record
            oneOf(mockGNService).makeCSWRecordInsertion(with(any(CSWRecord.class)));will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.insertRecord(jobId);
        Assert.assertNotNull(mav);
        Assert.assertFalse((Boolean) mav.getModel().get("success"));
    }
}