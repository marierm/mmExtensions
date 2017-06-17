+ Server {
	bootInRoom { arg startAliveThread=true, recover=false, onFailure, room;
		var resp;
		if (statusWatcher.serverRunning, { "server already running".inform; ^this });
		if (statusWatcher.serverBooting, { "server already booting".inform; ^this });

		statusWatcher.serverBooting = true;
		if(startAliveThread, { this.startAliveThread });
		if(recover) { this.newNodeAllocators } { this.newAllocators };
		statusWatcher.bootNotifyFirst = true;
		this.doWhenBooted({
			statusWatcher.serverBooting = false;
			if (sendQuit.isNil) {
				sendQuit = this.inProcess or: {this.isLocal};
			};

			if (this.inProcess) {
				serverInterface = ServerShmInterface(thisProcess.pid);
			} {
				if (isLocal) {
					serverInterface = ServerShmInterface(addr.port);
				}
			};

			this.initTree;
			if(volume.volume != 0.0) {
				volume.play;
			};
		}, onFailure: onFailure ? false);
		if (remoteControlled.not, {
			"You will have to manually boot remote server.".inform;
		},{
			this.bootServerAppInRoom(room);
		});
	}
	bootServerAppInRoom { |room|
		if (inProcess, {
			"booting internal".inform;
			this.bootInProcess;
			//alive = true;
			//this.serverRunning = true;
			pid = thisProcess.pid;
		},{
			if (serverInterface.notNil) {
				serverInterface.disconnect;
				serverInterface = nil;
			};
			
			(
				"ladish_control rnewapp" + room.asString +
				(program ++ options.asOptionsString(addr.port)).quote
			).unixCmd;
			pid = (
				"ps -A | egrep 'supernova|scsynth' | awk '{print $1;}'"
			).unixCmdGetStdOut.asInt;
			//unixCmd(program ++ options.asOptionsString(addr.port)).postln;
			("booting " ++ addr.port.asString).inform;
		});
	}
}
