/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.azure;

import java.io.IOException;
import java.net.URI;

import org.soyatec.windowsazure.blob.BlobStorageClient;
import org.soyatec.windowsazure.blob.IBlobContainer;
import org.soyatec.windowsazure.blob.IBlobProperties;
import org.soyatec.windowsazure.blob.IBlockBlob;
import org.soyatec.windowsazure.blob.internal.BlobContents;
import org.soyatec.windowsazure.blob.internal.BlobProperties;
import org.soyatec.windowsazure.blob.internal.RetryPolicies;
import org.soyatec.windowsazure.blob.io.BlobFileStream;
import org.soyatec.windowsazure.internal.util.TimeSpan;

public class AzureStorageUtils {

    private static final String BLOB_HOST_NAME = "http://blob.core.windows.net/";
    private static final String CONTENT_TYPE_FILE = "File";
    
    public static BlobStorageClient createStorageAccess(String strAccountName, String strAccountKey) {
        BlobStorageClient objBlobStorage = BlobStorageClient.create(
                URI.create(BLOB_HOST_NAME), false, strAccountName,
                strAccountKey);
        objBlobStorage.setRetryPolicy(RetryPolicies.retryN(1,
                TimeSpan.fromSeconds(5)));
        return objBlobStorage;
    }

    public static IBlockBlob createBlockBlob(
    		IBlobContainer container, 
    		String blobName,
            String sourceFile) throws IOException {
        return container.createBlockBlob(getBlobProperties(blobName), getBlobContent(sourceFile));
    }
    
    public static IBlockBlob updateBlockBlob(
    		IBlobContainer container, 
    		String blobName,
            String sourceFile) throws IOException {
        
        return container.updateBlockBlob(getBlobProperties(blobName), getBlobContent(sourceFile));
    }

    private static IBlobProperties getBlobProperties(String blobName) {
    	final IBlobProperties blobProperties = new BlobProperties(blobName);
        blobProperties.setContentType(CONTENT_TYPE_FILE);
        return blobProperties;
    }
    
	private static BlobContents getBlobContent(String sourceFile)
			throws IOException {
		return new BlobContents(new BlobFileStream(sourceFile));
	}

    
}
