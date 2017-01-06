/*
Wenity v1.5 - a Zenity clone written in Java

Copyright (c) 2012, 2013  Karoly Kalman  http://kksw.zzl.org/

This file is part of Wenity v1.5.

Wenity v1.5 is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Wenity v1.5 is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wenity v1.5.  If not, see <http://www.gnu.org/licenses/>.

*/

package wenity;

import wenity.modules.common.IWenityModule;
import wenity.modules.common.ModuleRequest;
import wenity.modules.common.ModuleResponse;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.log4j.Log4j2;


@Log4j2
public class Wenity
{

	
    private String responseFile = null;

	// main method: returns exitCode and optionally creates answer file
    public int doIt (String[] args)
    {
    	
        try
        {
        	
        	
            final ModuleRequest moduleRequest = parseArgs (args);

            final IWenityModule module = findModule (moduleRequest.getModuleName ());
            if (module == null)
            {
                Logger.error ("Can't find module " + moduleRequest.getModuleName ());
                return Constants.EXIT_STATUS_APP_ERROR;
            }

            final ModuleResponse moduleResponse = executeModule (module, moduleRequest);
            if (moduleResponse.isValid () && moduleResponse.createsResponseFile ())
            {
                // write response file only if user selected something
                writeResponseFile (moduleResponse.getResultAsString ());
                return Constants.EXIT_STATUS_OK;
            }

            return moduleResponse.getResultAsInt ();
        }
        catch (Exception ex)
        {
            Logger.exception ("Wenity - An error occurred! ", ex);
            return Constants.EXIT_STATUS_APP_ERROR;
        }
    }

    private ModuleRequest parseArgs (final String[] args) throws IOException, URISyntaxException
    {

    	OptionParser parser = new OptionParser();
    	
    	parser.accepts("debug","Debug level logging");
    	parser.accepts("info","Info level logging");
    	ArgumentAcceptingOptionSpec<String> responseOpt =	
    		parser.accepts("response-file","Resonse file - defaults to stdout").withRequiredArg().ofType(String.class);
        

    	if(args.length == 0) 
    		usage(parser,true);
    	try
        {
        	OptionSet opts = parser.parse(args);
        	if( opts.has("debug"))
        		Logger.goDebugMode();
        	if(opts.has("info"))
        		Logger.goInfoMode();
        	if(opts.has("help"))
        		usage(parser,true);
        	if(opts.has(responseOpt))
        		this.responseFile = responseOpt.value(opts);
        	@SuppressWarnings("unchecked")
			List<String> largs = (List<String>) opts.nonOptionArguments();
        	largs = new ArrayList<String>(largs);
        	final String moduleName = largs.remove(0);
            String[] moduleParams = largs.toArray(new String[0]);
            
            return new ModuleRequest (moduleName, moduleParams);
        }
        catch (final Exception ex)
        {
        	mLogger.warn(ex);
        	usage(parser, false);
        }
		return null;// snh
    }

	private void launchBrowser(String uri) throws IOException, URISyntaxException {
		if(  uri != null ){
			Desktop desktop = 
					Desktop.isDesktopSupported() ? Desktop.getDesktop() : null ; // Avoid exception 

					if( desktop != null ) 
						desktop.browse(new URI(uri));

		}
	}

	private void usage(OptionParser parser, boolean showBrowser) throws IOException, URISyntaxException {
		parser.printHelpOn(System.err);
		System.err.println("See: "+Constants.APP_DOCS_URL);
		if( showBrowser ) launchBrowser(Constants.APP_DOCS_URL);
		
		System.err.println("\nModules:\n");
        for (final IWenityModule module : ModuleConfig.getInstalledModules ())
        {
        	System.err.println(String.format("%-20s: %s",module.getModuleName(),module.getModuleDescription()));
        }
        
		System.exit(0);
		
	}

	private IWenityModule findModule (String moduleName)
    {
        for (final IWenityModule module : ModuleConfig.getInstalledModules ())
        {
            if (module.canProcess (moduleName))
            {
                return module;
            }
        }
        return null;
    }

    private ModuleResponse executeModule (IWenityModule module, ModuleRequest moduleRequest) throws Exception
    {
        try
        {
            Logger.debug ("Executing '%s' with request '%s'", module.getModuleName (), moduleRequest);

            final ModuleResponse moduleResponse = module.process (moduleRequest);

            Logger.debug ("Executed '%s' with response '%s'", module.getModuleName (), moduleResponse);
            return moduleResponse;
        }
        catch (Exception ex)
        {
            Logger.error ("Failed to execute '%s' with params '%s'", module.getModuleName (), moduleRequest);
            throw ex;
        }
    }

    private void writeResponseFile (final String data) throws IOException
    {
    	if( responseFile == null || responseFile.isEmpty() || responseFile.equals("-")){
    		System.out.println(data);
    		return ;
    	}
    		
    	
        PrintWriter printWriter = null;
        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        try
        {
            fileWriter = new FileWriter (responseFile);
            bufferedWriter = new BufferedWriter (fileWriter);
            printWriter = new PrintWriter (bufferedWriter);
            printWriter.println (data);
        }
        finally
        {
            Utils.closeNoThrow (printWriter);
            Utils.closeNoThrow (bufferedWriter);
            Utils.closeNoThrow (fileWriter);
        }
    }


}
