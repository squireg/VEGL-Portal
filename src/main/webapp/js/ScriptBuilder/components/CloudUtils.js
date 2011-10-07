/*
 * This file is part of the AuScope Virtual Exploration Geophysics Lab (VEGL) project.
 * Copyright (c) 2011 CSIRO Earth Science and Resource Engineering
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load( {url : ScriptBuilder.componentPath + "CloudUtils.json"});

CloudUtilsNode = Ext.extend(ScriptBuilder.BasePythonComponent, {

    constructor : function(container) {
        CloudUtilsNode.superclass.constructor.apply(this, [ container,
                "Cloud Utils Object", "CloudUtils", "s" ]);

        var numShells = container.getShellCommands().length;
        this.values.uniqueName = "CloudUtils" + numShells;
    },

    /**
     * This is where we dynamically generate a python Getter/Setter class from the job object that
     * is sent to us
     */
    getScript : function() {
        var text = '';

        text += '# ----- Autogenerated AWS Utility Functions -----' + this._newLine;
        text += '# Uploads inFilePath to the specified bucket with the specified key' + this._newLine;
        text += 'def cloudUpload(inFilePath, cloudBucket, cloudDir,cloudKey, ):' + this._newLine;
        text += this._tab + 'queryPath = (cloudBucket + "/" + cloudDir + "/" + cloudKey).replace("//", "/")' + this._newLine;
        text += this._tab + 'retcode = subprocess.call(["cloud", "upload", cloudBucket,cloudDir,cloudKey, inFilePath, "--set-acl=public-read"])' + this._newLine;
        text += this._tab + 'print "cloudUpload: " + inFilePath + " to " + queryPath + " returned " + str(retcode)' + this._newLine;
        text += this._newLine;
        text += '# downloads the specified key from bucket and writes it to outfile' + this._newLine;
        text += 'def cloudDownload(cloudBucket, cloudDir,cloudKey, outFilePath):' + this._newLine;
        text += this._tab + 'queryPath = (cloudBucket + "/" + cloudDir + "/" + cloudKey).replace("//", "/")' + this._newLine;
        text += this._tab + 'retcode = subprocess.call(["cloud", "download",cloudBucket,cloudDir,cloudKey, outFilePath])' + this._newLine;
        text += this._tab + 'print "cloudDownload: " + queryPath + " to " + outFilePath + " returned " + str(retcode)' + this._newLine;
        text += '# -----------------------------------------------' + this._newLine;
        text += this._newLine;

        return text;
    }
});