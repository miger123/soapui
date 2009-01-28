/*
 *  soapUI, copyright (C) 2004-2008 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.wsdl.actions.iface.tools.soapui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;

import org.apache.log4j.Logger;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlTestSuite;
import com.eviware.soapui.impl.wsdl.actions.iface.tools.support.AbstractToolsAction;
import com.eviware.soapui.impl.wsdl.actions.iface.tools.support.ArgumentBuilder;
import com.eviware.soapui.impl.wsdl.actions.iface.tools.support.ProcessToolRunner;
import com.eviware.soapui.impl.wsdl.actions.iface.tools.support.ToolHost;
import com.eviware.soapui.impl.wsdl.support.HelpUrls;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormDialogBuilder;
import com.eviware.x.form.XFormFactory;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldListener;

/**
 * Invokes soapUI TestRunner tool
 * 
 * @author Ole.Matzura
 */

public class TestRunnerAction extends AbstractToolsAction<WsdlProject>
{
	private static final String ALL_VALUE = "<all>";
	private static final String ENDPOINT = "Endpoint";
	private static final String HOSTPORT = "Host:Port";
	private static final String TESTSUITE = "TestSuite";
	private static final String TESTCASE = "TestCase";
	private static final String USERNAME = "Username";
	private static final String PASSWORD = "Password";
	private static final String WSSTYPE = "WSS Password Type";
	private static final String DOMAIN = "Domain";
	private static final String PRINTREPORT = "Print Report";
	private static final String ROOTFOLDER = "Root Folder";
	private static final String EXPORTJUNITRESULTS = "Export JUnit Results";
	private static final String EXPORTALL = "Export All";
	private static final String ENABLEUI = "Enable UI";
	private static final String TESTRUNNERPATH = "TestRunner Path";
	private static final String SAVEPROJECT = "Save Project";
	private static final String ADDSETTINGS = "Add Settings";
	private static final String OPEN_REPORT = "Open Report";
	private static final String COVERAGE = "Coverage Report";

	private XForm mainForm;

	private final static Logger log = Logger.getLogger(TestRunnerAction.class);

	public static final String SOAPUI_ACTION_ID = "TestRunnerAction";

	private XForm advForm;

	private List<TestSuite> testSuites;

	public TestRunnerAction()
	{
		super("Launch TestRunner", "Launch command-line TestRunner for this project");
	}

	protected XFormDialog buildDialog(WsdlProject modelItem)
	{
		if (modelItem == null)
			return null;

		XFormDialogBuilder builder = XFormFactory.createDialogBuilder("Launch TestRunner");

		mainForm = builder.createForm("Basic");
		mainForm.addComboBox(TESTSUITE, new String[] {}, "The TestSuite to run").addFormFieldListener(
				new XFormFieldListener()
				{

					public void valueChanged(XFormField sourceField, String newValue, String oldValue)
					{
						List<String> testCases = new ArrayList<String>();
						String tc = mainForm.getComponentValue(TESTCASE);

						if (newValue.equals(ALL_VALUE))
						{
							for (TestSuite testSuite : testSuites)
							{
								for (TestCase testCase : testSuite.getTestCaseList())
								{
									if (!testCases.contains(testCase.getName()))
										testCases.add(testCase.getName());
								}
							}
						}
						else
						{
							TestSuite testSuite = getModelItem().getTestSuiteByName(newValue);
							if (testSuite != null)
								testCases.addAll(Arrays.asList(ModelSupport.getNames(testSuite.getTestCaseList())));
						}

						testCases.add(0, ALL_VALUE);
						mainForm.setOptions(TESTCASE, testCases.toArray());

						if (testCases.contains(tc))
						{
							mainForm.getFormField(TESTCASE).setValue(tc);
						}
					}
				});

		mainForm.addComboBox(TESTCASE, new String[] {}, "The TestCase to run");
		mainForm.addSeparator();
		mainForm.addCheckBox(PRINTREPORT, "Prints a summary report to the console");
		mainForm.addCheckBox(EXPORTJUNITRESULTS, "Exports results to a JUnit-Style report");
		mainForm.addCheckBox(EXPORTALL, "Exports all results (not only errors)");
		mainForm.addTextField(ROOTFOLDER, "Folder to export to", XForm.FieldType.FOLDER);
		mainForm.addCheckBox(COVERAGE, "Generate WSDL Coverage report (soapUI Pro only)");
		mainForm.addCheckBox(OPEN_REPORT, "Open generated HTML report in browser (soapUI Pro only)");
		mainForm.addSeparator();
		mainForm.addCheckBox(ENABLEUI, "Enables UI components in scripts");
		mainForm.addTextField(TESTRUNNERPATH, "Folder containing TestRunner.bat to use", XForm.FieldType.FOLDER);
		mainForm.addCheckBox(SAVEPROJECT, "Saves project before running").setEnabled(!modelItem.isRemote());
		mainForm.addCheckBox(ADDSETTINGS, "Adds global settings to command-line");

		advForm = builder.createForm("Overrides");
		advForm.addComboBox(ENDPOINT, new String[] { "" }, "endpoint to forward to");
		advForm.addTextField(HOSTPORT, "Host:Port to use for requests", XForm.FieldType.TEXT);
		advForm.addSeparator();
		advForm.addTextField(USERNAME, "The username to set for all requests", XForm.FieldType.TEXT);
		advForm.addTextField(PASSWORD, "The password to set for all requests", XForm.FieldType.PASSWORD);
		advForm.addTextField(DOMAIN, "The domain to set for all requests", XForm.FieldType.TEXT);
		advForm.addComboBox(WSSTYPE, new String[] { "", "Text", "Digest" }, "The username to set for all requests");

		setToolsSettingsAction(null);
		buildArgsForm(builder, false, "TestRunner");

		return builder.buildDialog(buildDefaultActions(HelpUrls.TESTRUNNER_HELP_URL, modelItem),
				"Specify arguments for launching soapUI TestRunner", UISupport.TOOL_ICON);
	}

	protected Action createRunOption(WsdlProject modelItem)
	{
		Action action = super.createRunOption(modelItem);
		action.putValue(Action.NAME, "Launch");
		return action;
	}

	protected StringToStringMap initValues(WsdlProject modelItem, Object param)
	{
		if (modelItem != null && mainForm != null)
		{
			List<String> endpoints = new ArrayList<String>();

			for (Interface iface : modelItem.getInterfaceList())
			{
				for (String endpoint : iface.getEndpoints())
				{
					if (!endpoints.contains(endpoint))
						endpoints.add(endpoint);
				}
			}

			endpoints.add(0, null);
			advForm.setOptions(ENDPOINT, endpoints.toArray());

			testSuites = modelItem.getTestSuiteList();
			for (int c = 0; c < testSuites.size(); c++)
			{
				if (testSuites.get(c).getTestCaseCount() == 0)
				{
					testSuites.remove(c);
					c--;
				}
			}

			mainForm.setOptions(TESTSUITE, ModelSupport.getNames(new String[] { ALL_VALUE }, testSuites));

			List<String> testCases = new ArrayList<String>();

			for (TestSuite testSuite : testSuites)
			{
				for (TestCase testCase : testSuite.getTestCaseList())
				{
					if (!testCases.contains(testCase.getName()))
						testCases.add(testCase.getName());
				}
			}

			testCases.add(0, ALL_VALUE);
			mainForm.setOptions(TESTCASE, testCases.toArray());
		}
		else if (mainForm != null)
		{
			mainForm.setOptions(ENDPOINT, new String[] { null });
		}

		StringToStringMap values = super.initValues(modelItem, param);

		if (mainForm != null)
		{
			if (param instanceof WsdlTestCase)
			{
				mainForm.getFormField(TESTSUITE).setValue(((WsdlTestCase) param).getTestSuite().getName());
				mainForm.getFormField(TESTCASE).setValue(((WsdlTestCase) param).getName());

				values.put(TESTSUITE, ((WsdlTestCase) param).getTestSuite().getName());
				values.put(TESTCASE, ((WsdlTestCase) param).getName());
			}
			else if (param instanceof WsdlTestSuite)
			{
				mainForm.getFormField(TESTSUITE).setValue(((WsdlTestSuite) param).getName());
				values.put(TESTSUITE, ((WsdlTestSuite) param).getName());
			}
			
			mainForm.getComponent(SAVEPROJECT).setEnabled( !modelItem.isRemote());
		}

		return values;
	}

	protected void generate(StringToStringMap values, ToolHost toolHost, WsdlProject modelItem) throws Exception
	{
		String testRunnerDir = mainForm.getComponentValue(TESTRUNNERPATH);

		ProcessBuilder builder = new ProcessBuilder();
		ArgumentBuilder args = buildArgs(modelItem);
		builder.command(args.getArgs());
		if (StringUtils.isNullOrEmpty(testRunnerDir))
			builder.directory(new File("."));
		else
			builder.directory(new File(testRunnerDir));

		if (mainForm.getComponentValue(SAVEPROJECT).equals(Boolean.TRUE.toString()))
		{
			modelItem.save();
		}
		else if( StringUtils.isNullOrEmpty( modelItem.getPath() ))
		{
			UISupport.showErrorMessage("Project [" + modelItem.getName() + "] has not been saved to file." );
			return;
		}

		if (log.isDebugEnabled())
			log.debug("Launching testrunner in directory [" + builder.directory() + "] with arguments [" + args.toString()
					+ "]");

		toolHost.run(new ProcessToolRunner(builder, "soapUI TestRunner", modelItem, args));
	}

	private ArgumentBuilder buildArgs(WsdlProject modelItem) throws IOException
	{
		if (dialog == null)
		{
			ArgumentBuilder builder = new ArgumentBuilder(new StringToStringMap());
			builder.startScript("testrunner", ".bat", ".sh");
			return builder;
		}

		StringToStringMap values = dialog.getValues();

		ArgumentBuilder builder = new ArgumentBuilder(values);

		builder.startScript("testrunner", ".bat", ".sh");

		builder.addString(ENDPOINT, "-e", "");
		builder.addString(HOSTPORT, "-h", "");

		if (!values.get(TESTSUITE).equals(ALL_VALUE))
			builder.addString(TESTSUITE, "-s", "");

		if (!values.get(TESTCASE).equals(ALL_VALUE))
			builder.addString(TESTCASE, "-c", "");

		builder.addString(USERNAME, "-u", "");
		builder.addStringShadow(PASSWORD, "-p", "");
		builder.addString(DOMAIN, "-d", "");
		builder.addString(WSSTYPE, "-w", "");

		builder.addBoolean(PRINTREPORT, "-r");
		builder.addBoolean(EXPORTALL, "-a");
		builder.addBoolean(EXPORTJUNITRESULTS, "-j");
		builder.addString(ROOTFOLDER, "-f");
		builder.addBoolean(OPEN_REPORT, "-o");
		builder.addBoolean(COVERAGE, "-g");

		if (dialog.getBooleanValue(ADDSETTINGS))
		{
			try
			{
				builder.addBoolean(ADDSETTINGS, "-t" + SoapUI.saveSettings());
			}
			catch (Exception e)
			{
				SoapUI.logError(e);
			}
		}

		builder.addBoolean(ENABLEUI, "-i");
		builder.addArgs(new String[] { modelItem.getPath() });

		addToolArgs(values, builder);

		return builder;
	}
}
