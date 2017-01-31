/**
 * 
 */
package org.athento.nuxeo.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.operations.InheritMetadataOperation;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Propagate metadata worker.
 */
public class PropagateMetadataWorker extends AbstractWork {

	private static final long serialVersionUID = -518077422027928515L;

	/** Log. */
	private static Log LOG = LogFactory.getLog(PropagateMetadataWorker.class);

	/**
	 * Category.
	 */
	private static final String CATEGORY = "inheritance";

	/** Ignored metadatas. */
	private String ignoredMetadatas;

	/**
	 * Constructor.
	 *
	 * @param repositoryName
	 * @param docId
     */
	public PropagateMetadataWorker(String repositoryName, String docId) {
		super(repositoryName + ':' + docId + ":inheritance");
		setDocument(repositoryName, docId);
	}

	@Override
	public String getTitle() {
		return getCategory();
	}
	@Override
	public String getCategory() {
		return CATEGORY;
	}

	@Override
	public void work() throws Exception {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting inherit worker...");
        }
		initSession();
		if (!session.exists(new IdRef(docId))) {
            LOG.info("Document " + docId + " is not found for inheritance.");
			setStatus("Nothing to process");
			return;
		}
		float percent = 0;
		setProgress(new Progress(percent));
		try {
			setStatus("Propagating metadata");
			// Execute operation
			InheritMetadataOperation op = new InheritMetadataOperation();
			try {
				op.setSession(session);
				op.setParamIgnoreMetadatas(ignoredMetadatas);
				// FIX: Add only schemas here if it is necessary
				DocumentModel doc = session.getDocument(new IdRef(docId));
				op.run(doc);
                // Save document
                session.saveDocument(doc);
			} catch (Exception e) {
				throw new ClientException("Unable to execute inherit worker", e);
			}
		} finally {
			commitOrRollbackTransaction();
			startTransaction();
			setProgress(new Progress(100));
			setStatus("Finished");
		}
	}

    public String getIgnoredMetadatas() {
		return ignoredMetadatas;
	}

	public void setIgnoredMetadatas(String ignoredMetadatas) {
		this.ignoredMetadatas = ignoredMetadatas;
	}

}
