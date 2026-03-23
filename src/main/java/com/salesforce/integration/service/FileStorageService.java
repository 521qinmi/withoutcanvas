package com.salesforce.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    
    private static final String STORAGE_DIR = System.getProperty("java.io.tmpdir") + "/salesforce-form-data";
    private final ObjectMapper objectMapper;
    
    public FileStorageService() {
        this.objectMapper = new ObjectMapper();
        initializeStorageDirectory();
    }
    
    /**
     * 初始化存储目录
     */
    private void initializeStorageDirectory() {
        try {
            Path storagePath = Paths.get(STORAGE_DIR);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                logger.info("Created storage directory: {}", STORAGE_DIR);
            }
        } catch (IOException e) {
            logger.error("Failed to create storage directory", e);
        }
    }
    
    /**
     * 保存账户表单数据到文件
     * @param recordId Salesforce Record ID
     * @param formData 表单数据
     * @return 是否保存成功
     */
    public boolean saveAccountData(String recordId, Map<String, Object> formData) {
        try {
            String fileName = getFileName(recordId);
            Path filePath = Paths.get(fileName);
            
            // 转换为 JSON 并保存
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(formData);
            Files.writeString(filePath, json);
            
            logger.info("Saved account data for recordId: {} to {}", recordId, fileName);
            return true;
        } catch (IOException e) {
            logger.error("Failed to save account data for recordId: {}", recordId, e);
            return false;
        }
    }
    
    /**
     * 从文件读取账户表单数据
     * @param recordId Salesforce Record ID
     * @return 表单数据，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadAccountData(String recordId) {
        try {
            String fileName = getFileName(recordId);
            Path filePath = Paths.get(fileName);
            
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                Map<String, Object> data = objectMapper.readValue(json, Map.class);
                logger.info("Loaded account data for recordId: {} from {}", recordId, fileName);
                return data;
            } else {
                logger.info("No saved data found for recordId: {}", recordId);
                return null;
            }
        } catch (IOException e) {
            logger.error("Failed to load account data for recordId: {}", recordId, e);
            return null;
        }
    }
    
    /**
     * 检查是否存在已保存的数据
     * @param recordId Salesforce Record ID
     * @return true 如果存在已保存的数据
     */
    public boolean hasSavedData(String recordId) {
        String fileName = getFileName(recordId);
        Path filePath = Paths.get(fileName);
        return Files.exists(filePath);
    }
    
    /**
     * 删除已保存的数据
     * @param recordId Salesforce Record ID
     * @return 是否删除成功
     */
    public boolean deleteAccountData(String recordId) {
        try {
            String fileName = getFileName(recordId);
            Path filePath = Paths.get(fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted account data for recordId: {}", recordId);
                return true;
            } else {
                logger.warn("Attempted to delete non-existent data for recordId: {}", recordId);
                return false;
            }
        } catch (IOException e) {
            logger.error("Failed to delete account data for recordId: {}", recordId, e);
            return false;
        }
    }
    
    /**
     * 获取所有已保存的记录 ID
     * @return 记录 ID 列表
     */
    public Map<String, Long> getAllSavedRecords() {
        Map<String, Long> records = new HashMap<>();
        try {
            Path storagePath = Paths.get(STORAGE_DIR);
            if (Files.exists(storagePath) && Files.isDirectory(storagePath)) {
                Files.list(storagePath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String recordId = fileName.replace(".json", "");
                        try {
                            long lastModified = Files.getLastModifiedTime(path).toMillis();
                            records.put(recordId, lastModified);
                        } catch (IOException e) {
                            logger.error("Failed to get last modified time for {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            logger.error("Failed to list saved records", e);
        }
        return records;
    }
    
    /**
     * 根据 Record ID 生成文件名
     */
    private String getFileName(String recordId) {
        // 清理 recordId 中的特殊字符，确保文件名安全
        String cleanRecordId = recordId.replaceAll("[^a-zA-Z0-9]", "_");
        return STORAGE_DIR + "/account_" + cleanRecordId + ".json";
    }
    
    /**
     * 获取存储目录路径
     */
    public String getStorageDirectory() {
        return STORAGE_DIR;
    }
}