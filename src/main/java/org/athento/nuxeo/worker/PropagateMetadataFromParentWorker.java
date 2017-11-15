/**
 * 
 */
package org.athento.nuxeo.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.operations.InheritMetadataFromParentOperation;
import org.athento.nuxeo.operations.InheritMetadataOperation;
import org.athento.nuxeo.utils.InheritUtil;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Propagate metadata from parent worker.
 */
public class PropagateMetadataFromParentWorker extends AbstractWork {

	private static final long serialVersionUID = -523452355111928515L;

	/** Log. */
	private static Log LOG = LogFactory.getLog(PropagateMetadataFromParentWorker.class);

	/**
	 * Category.
	 */
	private static final String CATEGORY = "inheritance";

	/** Ignored versions. */
	private boolean ignoreVersions;

	/**
	 * Update documents for the worker.
	 */
	public final List<DocumentModel> updatedDocuments = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param repositoryName
	 * @param docId
	 */
	public PropagateMetadataFromParentWorker(String repositoryName, String docId) {
		super(repositoryName + ':' + docId + ":parentInheritance");
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
	public void work() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting inherit parent worker...");
        }
		openSystemSession();
		if (!session.exists(new IdRef(docId))) {
			setStatus("Nothing to process");
			return;
		}
		float percent = 0;
		setProgress(new Progress(percent));
		try {
			setStatus("Propagating metadata from parent");
			// Execute operation
			InheritMetadataFromParentOperation op = new InheritMetadataFromParentOperation();
			op.setIgnoreVersions(ignoreVersions);
			op.setSession(session);
			DocumentModel doc = session.getDocument(new IdRef(docId));
			op.run(doc);
		} catch (Exception e) {
			LOG.error("Unable to execute propagate metadata from parent", e);
		} finally {
			commitOrRollbackTransaction();
			startTransaction();
			setProgress(new Progress(100));
			setStatus("Finished");
		}
	}

	public boolean isIgnoreVersions() {
		return ignoreVersions;
	}

	public void setIgnoreVersions(boolean ignoreVersions) {
		this.ignoreVersions = ignoreVersions;
	}

}
