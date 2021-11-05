{{/*
Expand the name of the chart.
*/}}
{{- define "trexis-backbase-identity-ingestion-authenticator.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "trexis-backbase-identity-ingestion-authenticator.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "trexis-backbase-identity-ingestion-authenticator.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "trexis-backbase-identity-ingestion-authenticator.labels" -}}
helm.sh/chart: {{ include "trexis-backbase-identity-ingestion-authenticator.chart" . }}
{{ include "trexis-backbase-identity-ingestion-authenticator.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "trexis-backbase-identity-ingestion-authenticator.selectorLabels" -}}
app.backbase.com/tier: trexis
app.kubernetes.io/instance: westerra-trexis-backbase-identity-ingestion-authenticator
app.kubernetes.io/name: trexis-backbase-identity-ingestion-authenticator
app.kubernetes.io/part-of: westerra
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "trexis-backbase-identity-ingestion-authenticator.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "trexis-backbase-identity-ingestion-authenticator.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
