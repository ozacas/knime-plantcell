
package compbio.data.msa._01._12._2010;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the compbio.data.msa._01._12._2010 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _LimitExceededException_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "LimitExceededException");
    private final static QName _WrongParameterException_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "WrongParameterException");
    private final static QName _GetLimits_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getLimits");
    private final static QName _CancelJob_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "cancelJob");
    private final static QName _PullExecStatistics_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "pullExecStatistics");
    private final static QName _GetJobStatus_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getJobStatus");
    private final static QName _GetRunnerOptions_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getRunnerOptions");
    private final static QName _GetPresets_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getPresets");
    private final static QName _CustomAnalizeResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "customAnalizeResponse");
    private final static QName _PresetAnalize_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "presetAnalize");
    private final static QName _JobSubmissionException_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "JobSubmissionException");
    private final static QName _UnsupportedRuntimeException_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "UnsupportedRuntimeException");
    private final static QName _CancelJobResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "cancelJobResponse");
    private final static QName _GetLimitsResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getLimitsResponse");
    private final static QName _Analize_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "analize");
    private final static QName _PresetAnalizeResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "presetAnalizeResponse");
    private final static QName _RunnerConfig_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "runnerConfig");
    private final static QName _GetAnnotation_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getAnnotation");
    private final static QName _GetLimitResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getLimitResponse");
    private final static QName _Limits_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "limits");
    private final static QName _GetLimit_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getLimit");
    private final static QName _GetAnnotationResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getAnnotationResponse");
    private final static QName _Presets_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "presets");
    private final static QName _GetRunnerOptionsResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getRunnerOptionsResponse");
    private final static QName _CustomAnalize_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "customAnalize");
    private final static QName _ResultNotAvailableException_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "ResultNotAvailableException");
    private final static QName _PullExecStatisticsResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "pullExecStatisticsResponse");
    private final static QName _AnalizeResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "analizeResponse");
    private final static QName _GetJobStatusResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getJobStatusResponse");
    private final static QName _GetPresetsResponse_QNAME = new QName("http://msa.data.compbio/01/12/2010/", "getPresetsResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: compbio.data.msa._01._12._2010
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Score }
     * 
     */
    public Score createScore() {
        return new Score();
    }

    /**
     * Create an instance of {@link CustomAnalizeResponse }
     * 
     */
    public CustomAnalizeResponse createCustomAnalizeResponse() {
        return new CustomAnalizeResponse();
    }

    /**
     * Create an instance of {@link ValueConstrain }
     * 
     */
    public ValueConstrain createValueConstrain() {
        return new ValueConstrain();
    }

    /**
     * Create an instance of {@link Analize }
     * 
     */
    public Analize createAnalize() {
        return new Analize();
    }

    /**
     * Create an instance of {@link PullExecStatistics }
     * 
     */
    public PullExecStatistics createPullExecStatistics() {
        return new PullExecStatistics();
    }

    /**
     * Create an instance of {@link ResultNotAvailableException }
     * 
     */
    public ResultNotAvailableException createResultNotAvailableException() {
        return new ResultNotAvailableException();
    }

    /**
     * Create an instance of {@link GetRunnerOptionsResponse }
     * 
     */
    public GetRunnerOptionsResponse createGetRunnerOptionsResponse() {
        return new GetRunnerOptionsResponse();
    }

    /**
     * Create an instance of {@link GetPresetsResponse }
     * 
     */
    public GetPresetsResponse createGetPresetsResponse() {
        return new GetPresetsResponse();
    }

    /**
     * Create an instance of {@link Option }
     * 
     */
    public Option createOption() {
        return new Option();
    }

    /**
     * Create an instance of {@link PresetAnalizeResponse }
     * 
     */
    public PresetAnalizeResponse createPresetAnalizeResponse() {
        return new PresetAnalizeResponse();
    }

    /**
     * Create an instance of {@link Preset }
     * 
     */
    public Preset createPreset() {
        return new Preset();
    }

    /**
     * Create an instance of {@link PullExecStatisticsResponse }
     * 
     */
    public PullExecStatisticsResponse createPullExecStatisticsResponse() {
        return new PullExecStatisticsResponse();
    }

    /**
     * Create an instance of {@link FastaSequence }
     * 
     */
    public FastaSequence createFastaSequence() {
        return new FastaSequence();
    }

    /**
     * Create an instance of {@link Parameter }
     * 
     */
    public Parameter createParameter() {
        return new Parameter();
    }

    /**
     * Create an instance of {@link UnsupportedRuntimeException }
     * 
     */
    public UnsupportedRuntimeException createUnsupportedRuntimeException() {
        return new UnsupportedRuntimeException();
    }

    /**
     * Create an instance of {@link JobSubmissionException }
     * 
     */
    public JobSubmissionException createJobSubmissionException() {
        return new JobSubmissionException();
    }

    /**
     * Create an instance of {@link GetAnnotationResponse }
     * 
     */
    public GetAnnotationResponse createGetAnnotationResponse() {
        return new GetAnnotationResponse();
    }

    /**
     * Create an instance of {@link Preset.Optlist }
     * 
     */
    public Preset.Optlist createPresetOptlist() {
        return new Preset.Optlist();
    }

    /**
     * Create an instance of {@link Range }
     * 
     */
    public Range createRange() {
        return new Range();
    }

    /**
     * Create an instance of {@link ScoreHolder }
     * 
     */
    public ScoreHolder createScoreHolder() {
        return new ScoreHolder();
    }

    /**
     * Create an instance of {@link CancelJob }
     * 
     */
    public CancelJob createCancelJob() {
        return new CancelJob();
    }

    /**
     * Create an instance of {@link Limit }
     * 
     */
    public Limit createLimit() {
        return new Limit();
    }

    /**
     * Create an instance of {@link GetRunnerOptions }
     * 
     */
    public GetRunnerOptions createGetRunnerOptions() {
        return new GetRunnerOptions();
    }

    /**
     * Create an instance of {@link GetLimit }
     * 
     */
    public GetLimit createGetLimit() {
        return new GetLimit();
    }

    /**
     * Create an instance of {@link GetLimitResponse }
     * 
     */
    public GetLimitResponse createGetLimitResponse() {
        return new GetLimitResponse();
    }

    /**
     * Create an instance of {@link GetJobStatusResponse }
     * 
     */
    public GetJobStatusResponse createGetJobStatusResponse() {
        return new GetJobStatusResponse();
    }

    /**
     * Create an instance of {@link PresetAnalize }
     * 
     */
    public PresetAnalize createPresetAnalize() {
        return new PresetAnalize();
    }

    /**
     * Create an instance of {@link GetLimitsResponse }
     * 
     */
    public GetLimitsResponse createGetLimitsResponse() {
        return new GetLimitsResponse();
    }

    /**
     * Create an instance of {@link ChunkHolder }
     * 
     */
    public ChunkHolder createChunkHolder() {
        return new ChunkHolder();
    }

    /**
     * Create an instance of {@link LimitsManager }
     * 
     */
    public LimitsManager createLimitsManager() {
        return new LimitsManager();
    }

    /**
     * Create an instance of {@link GetLimits }
     * 
     */
    public GetLimits createGetLimits() {
        return new GetLimits();
    }

    /**
     * Create an instance of {@link GetPresets }
     * 
     */
    public GetPresets createGetPresets() {
        return new GetPresets();
    }

    /**
     * Create an instance of {@link WrongParameterException }
     * 
     */
    public WrongParameterException createWrongParameterException() {
        return new WrongParameterException();
    }

    /**
     * Create an instance of {@link LimitExceededException }
     * 
     */
    public LimitExceededException createLimitExceededException() {
        return new LimitExceededException();
    }

    /**
     * Create an instance of {@link PresetManager }
     * 
     */
    public PresetManager createPresetManager() {
        return new PresetManager();
    }

    /**
     * Create an instance of {@link CustomAnalize }
     * 
     */
    public CustomAnalize createCustomAnalize() {
        return new CustomAnalize();
    }

    /**
     * Create an instance of {@link ScoreManager }
     * 
     */
    public ScoreManager createScoreManager() {
        return new ScoreManager();
    }

    /**
     * Create an instance of {@link GetAnnotation }
     * 
     */
    public GetAnnotation createGetAnnotation() {
        return new GetAnnotation();
    }

    /**
     * Create an instance of {@link AnalizeResponse }
     * 
     */
    public AnalizeResponse createAnalizeResponse() {
        return new AnalizeResponse();
    }

    /**
     * Create an instance of {@link CancelJobResponse }
     * 
     */
    public CancelJobResponse createCancelJobResponse() {
        return new CancelJobResponse();
    }

    /**
     * Create an instance of {@link GetJobStatus }
     * 
     */
    public GetJobStatus createGetJobStatus() {
        return new GetJobStatus();
    }

    /**
     * Create an instance of {@link RunnerConfig }
     * 
     */
    public RunnerConfig createRunnerConfig() {
        return new RunnerConfig();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LimitExceededException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "LimitExceededException")
    public JAXBElement<LimitExceededException> createLimitExceededException(LimitExceededException value) {
        return new JAXBElement<LimitExceededException>(_LimitExceededException_QNAME, LimitExceededException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WrongParameterException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "WrongParameterException")
    public JAXBElement<WrongParameterException> createWrongParameterException(WrongParameterException value) {
        return new JAXBElement<WrongParameterException>(_WrongParameterException_QNAME, WrongParameterException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLimits }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getLimits")
    public JAXBElement<GetLimits> createGetLimits(GetLimits value) {
        return new JAXBElement<GetLimits>(_GetLimits_QNAME, GetLimits.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelJob }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "cancelJob")
    public JAXBElement<CancelJob> createCancelJob(CancelJob value) {
        return new JAXBElement<CancelJob>(_CancelJob_QNAME, CancelJob.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PullExecStatistics }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "pullExecStatistics")
    public JAXBElement<PullExecStatistics> createPullExecStatistics(PullExecStatistics value) {
        return new JAXBElement<PullExecStatistics>(_PullExecStatistics_QNAME, PullExecStatistics.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetJobStatus }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getJobStatus")
    public JAXBElement<GetJobStatus> createGetJobStatus(GetJobStatus value) {
        return new JAXBElement<GetJobStatus>(_GetJobStatus_QNAME, GetJobStatus.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetRunnerOptions }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getRunnerOptions")
    public JAXBElement<GetRunnerOptions> createGetRunnerOptions(GetRunnerOptions value) {
        return new JAXBElement<GetRunnerOptions>(_GetRunnerOptions_QNAME, GetRunnerOptions.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPresets }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getPresets")
    public JAXBElement<GetPresets> createGetPresets(GetPresets value) {
        return new JAXBElement<GetPresets>(_GetPresets_QNAME, GetPresets.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CustomAnalizeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "customAnalizeResponse")
    public JAXBElement<CustomAnalizeResponse> createCustomAnalizeResponse(CustomAnalizeResponse value) {
        return new JAXBElement<CustomAnalizeResponse>(_CustomAnalizeResponse_QNAME, CustomAnalizeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PresetAnalize }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "presetAnalize")
    public JAXBElement<PresetAnalize> createPresetAnalize(PresetAnalize value) {
        return new JAXBElement<PresetAnalize>(_PresetAnalize_QNAME, PresetAnalize.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JobSubmissionException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "JobSubmissionException")
    public JAXBElement<JobSubmissionException> createJobSubmissionException(JobSubmissionException value) {
        return new JAXBElement<JobSubmissionException>(_JobSubmissionException_QNAME, JobSubmissionException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UnsupportedRuntimeException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "UnsupportedRuntimeException")
    public JAXBElement<UnsupportedRuntimeException> createUnsupportedRuntimeException(UnsupportedRuntimeException value) {
        return new JAXBElement<UnsupportedRuntimeException>(_UnsupportedRuntimeException_QNAME, UnsupportedRuntimeException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CancelJobResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "cancelJobResponse")
    public JAXBElement<CancelJobResponse> createCancelJobResponse(CancelJobResponse value) {
        return new JAXBElement<CancelJobResponse>(_CancelJobResponse_QNAME, CancelJobResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLimitsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getLimitsResponse")
    public JAXBElement<GetLimitsResponse> createGetLimitsResponse(GetLimitsResponse value) {
        return new JAXBElement<GetLimitsResponse>(_GetLimitsResponse_QNAME, GetLimitsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Analize }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "analize")
    public JAXBElement<Analize> createAnalize(Analize value) {
        return new JAXBElement<Analize>(_Analize_QNAME, Analize.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PresetAnalizeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "presetAnalizeResponse")
    public JAXBElement<PresetAnalizeResponse> createPresetAnalizeResponse(PresetAnalizeResponse value) {
        return new JAXBElement<PresetAnalizeResponse>(_PresetAnalizeResponse_QNAME, PresetAnalizeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RunnerConfig }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "runnerConfig")
    public JAXBElement<RunnerConfig> createRunnerConfig(RunnerConfig value) {
        return new JAXBElement<RunnerConfig>(_RunnerConfig_QNAME, RunnerConfig.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAnnotation }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getAnnotation")
    public JAXBElement<GetAnnotation> createGetAnnotation(GetAnnotation value) {
        return new JAXBElement<GetAnnotation>(_GetAnnotation_QNAME, GetAnnotation.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLimitResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getLimitResponse")
    public JAXBElement<GetLimitResponse> createGetLimitResponse(GetLimitResponse value) {
        return new JAXBElement<GetLimitResponse>(_GetLimitResponse_QNAME, GetLimitResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LimitsManager }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "limits")
    public JAXBElement<LimitsManager> createLimits(LimitsManager value) {
        return new JAXBElement<LimitsManager>(_Limits_QNAME, LimitsManager.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLimit }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getLimit")
    public JAXBElement<GetLimit> createGetLimit(GetLimit value) {
        return new JAXBElement<GetLimit>(_GetLimit_QNAME, GetLimit.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAnnotationResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getAnnotationResponse")
    public JAXBElement<GetAnnotationResponse> createGetAnnotationResponse(GetAnnotationResponse value) {
        return new JAXBElement<GetAnnotationResponse>(_GetAnnotationResponse_QNAME, GetAnnotationResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PresetManager }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "presets")
    public JAXBElement<PresetManager> createPresets(PresetManager value) {
        return new JAXBElement<PresetManager>(_Presets_QNAME, PresetManager.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetRunnerOptionsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getRunnerOptionsResponse")
    public JAXBElement<GetRunnerOptionsResponse> createGetRunnerOptionsResponse(GetRunnerOptionsResponse value) {
        return new JAXBElement<GetRunnerOptionsResponse>(_GetRunnerOptionsResponse_QNAME, GetRunnerOptionsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CustomAnalize }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "customAnalize")
    public JAXBElement<CustomAnalize> createCustomAnalize(CustomAnalize value) {
        return new JAXBElement<CustomAnalize>(_CustomAnalize_QNAME, CustomAnalize.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResultNotAvailableException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "ResultNotAvailableException")
    public JAXBElement<ResultNotAvailableException> createResultNotAvailableException(ResultNotAvailableException value) {
        return new JAXBElement<ResultNotAvailableException>(_ResultNotAvailableException_QNAME, ResultNotAvailableException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PullExecStatisticsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "pullExecStatisticsResponse")
    public JAXBElement<PullExecStatisticsResponse> createPullExecStatisticsResponse(PullExecStatisticsResponse value) {
        return new JAXBElement<PullExecStatisticsResponse>(_PullExecStatisticsResponse_QNAME, PullExecStatisticsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AnalizeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "analizeResponse")
    public JAXBElement<AnalizeResponse> createAnalizeResponse(AnalizeResponse value) {
        return new JAXBElement<AnalizeResponse>(_AnalizeResponse_QNAME, AnalizeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetJobStatusResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getJobStatusResponse")
    public JAXBElement<GetJobStatusResponse> createGetJobStatusResponse(GetJobStatusResponse value) {
        return new JAXBElement<GetJobStatusResponse>(_GetJobStatusResponse_QNAME, GetJobStatusResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPresetsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://msa.data.compbio/01/12/2010/", name = "getPresetsResponse")
    public JAXBElement<GetPresetsResponse> createGetPresetsResponse(GetPresetsResponse value) {
        return new JAXBElement<GetPresetsResponse>(_GetPresetsResponse_QNAME, GetPresetsResponse.class, null, value);
    }

}
