/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.report.service;

import java.io.ByteArrayOutputStream;

import java.util.Locale;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;

import org.apache.fineract.infrastructure.dataqueries.service.ReadReportingService;
import org.apache.fineract.infrastructure.report.annotation.ReportService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@ReportService(type = { "Pentaho" })
public class PentahoReportingProcessServiceImpl implements ReportingProcessService {

    private final ReadReportingService readReportingService;
    private final PlatformSecurityContext context;

    @Autowired
    public PentahoReportingProcessServiceImpl(final ReadReportingService readReportingService,
            final PlatformSecurityContext context) {
        this.readReportingService = readReportingService;
        this.context = context;
    }

    @Override
    public Response processRequest(String reportName, MultivaluedMap<String, String> queryParams) {
        final String outputTypeParam = queryParams.getFirst("output-type");
        final String outputType = outputTypeParam != null ? outputTypeParam : "HTML";
        Map<String, String> reportParams = getReportParams(queryParams);
        
        // Get current user
        final AppUser currentUser = context.authenticatedUser();
        
        // Get locale from query params or use default
        Locale locale = ApiParameterHelper.extractLocale(queryParams);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        
        // Create error log StringBuilder
        StringBuilder errorLog = new StringBuilder();

        // Generate the Pentaho report
        ByteArrayOutputStream outputStream = readReportingService.generatePentahoReportAsOutputStream(reportName, outputType, reportParams, locale, currentUser, errorLog);

        // Set the content type based on output type
        String contentType = getContentType(outputType);

        // Build the response
        ResponseBuilder response = Response.ok(outputStream.toByteArray());
        response.header("Content-Type", contentType);
        response.header("Content-Disposition", "attachment; filename=\"" + reportName + "." + getFileExtension(outputType) + "\"");

        return response.build();
    }

    private String getContentType(String outputType) {
        switch (outputType.toUpperCase()) {
            case "HTML":
                return "text/html";
            case "PDF":
                return "application/pdf";
            case "XLS":
                return "application/vnd.ms-excel";
            case "XLSX":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "CSV":
                return "text/csv";
            default:
                return "text/plain";
        }
    }

    private String getFileExtension(String outputType) {
        switch (outputType.toUpperCase()) {
            case "HTML":
                return "html";
            case "PDF":
                return "pdf";
            case "XLS":
                return "xls";
            case "XLSX":
                return "xlsx";
            case "CSV":
                return "csv";
            default:
                return "txt";
        }
    }
}
