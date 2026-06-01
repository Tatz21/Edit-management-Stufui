package com.example.domain.usecase

import android.content.Context
import com.example.domain.model.Project
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class ExportXlsxUseCase {

    fun execute(context: Context, filename: String, projects: List<Project>): Result<File> {
        return try {
            val file = File(context.cacheDir, "$filename.csv")
            if (file.exists()) {
                file.delete()
            }
            
            val headers = listOf(
                "Client Name", "Project Name", "Description", "Assigned Editor", 
                "Status", "Priority", "Total Amount", "Advance Amount", 
                "Remaining Amount", "Deadline", "Preview Status", "Received Date"
            )

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Write BOM for Excel UTF-8 encoding compatibility
                    writer.write('\ufeff'.code)
                    
                    // Write headers
                    writer.write(headers.joinToString(",") { escapeCsv(it) } + "\n")
                    
                    // Write rows
                    projects.forEach { project ->
                        val row = listOf(
                            project.clientName,
                            project.projectTitle,
                            project.description,
                            project.assignedEditor,
                            project.status,
                            project.priority,
                            project.totalAmount.toString(),
                            project.advanceAmount.toString(),
                            project.remainingAmount.toString(),
                            project.deadlineDate,
                            if (project.previewApproved) "Approved" else "Pending",
                            project.receivedDate
                        )
                        writer.write(row.joinToString(",") { escapeCsv(it) } + "\n")
                    }
                    writer.flush()
                }
            }

            Result.success(file)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: Any?): String {
        val str = value?.toString() ?: ""
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\""
        }
        return str
    }
}
