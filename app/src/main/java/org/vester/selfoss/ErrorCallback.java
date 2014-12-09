package org.vester.selfoss;

import org.vester.selfoss.operation.Operation;

public interface ErrorCallback {

	void errorOccured(String url, Operation operation, Exception e);

}
