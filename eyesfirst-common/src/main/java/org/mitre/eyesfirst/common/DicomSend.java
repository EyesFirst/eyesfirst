/*
 * Copyright 2013 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.eyesfirst.common;

import java.io.File;

import org.dcm4che2.tool.dcmsnd.DcmSnd;

/**
 * Currently this just wraps DcmSnd to deal with sending the file. A future
 * version might be a little less pathetic.
 * @author dpotter
 */
public class DicomSend {
	// TODO: Configure these or something?
	private static final String DEFAULT_HOSTNAME = "localhost";
	private static final int DEFAULT_PORT = 11112;
	private final DcmSnd dcmsnd;
	/*
	private final NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();
	private final NetworkConnection remoteConn = new NetworkConnection();
	private Device device;
	*/

	public DicomSend(String deviceName) {
		dcmsnd = new DcmSnd(deviceName);
	}

	public DicomSend() {
		this("EYESFIRST");
	}

	public void setDicomURL(String url) {
		// Parse the URL (sort of)
		int i = url.indexOf('@');
		dcmsnd.setCalledAET(i >= 0 ? url.substring(0, i) : url);
		if (i >= 0) {
			// Also have host/port info, maybe
			String connection = url.substring(i+1);
			i = connection.indexOf(':');
			dcmsnd.setRemoteHost(i >= 0 ? connection.substring(0, i) : connection);
			if (i < 0) {
				dcmsnd.setRemotePort(DEFAULT_PORT);
			} else {
				int port = DEFAULT_PORT;
				try {
					port = Integer.parseInt(connection.substring(i+1));
					if (port < 1 || port > 0xFFFF) {
						// Reset to default
						port = DEFAULT_PORT;
					}
				} catch (NumberFormatException e) {
					// Currently, just eat it and fall through
				}
				dcmsnd.setRemotePort(port);
			}
		} else {
			dcmsnd.setRemoteHost(DEFAULT_HOSTNAME);
			dcmsnd.setRemotePort(DEFAULT_PORT);
		}
	}

	public void sendFile(File file) throws Exception {
		dcmsnd.addFile(file);

		dcmsnd.setOfferDefaultTransferSyntaxInSeparatePresentationContext(false);
		dcmsnd.setSendFileRef(false);
		dcmsnd.setStorageCommitment(false);
		dcmsnd.setPackPDV(true);
		dcmsnd.setTcpNoDelay(true);

		dcmsnd.configureTransferCapability();
		dcmsnd.start();

		try {
			dcmsnd.open();
			dcmsnd.send();
			dcmsnd.close();
		} finally {
			dcmsnd.stop();
		}
	}
}
