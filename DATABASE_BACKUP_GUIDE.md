# 🗄️ BSK Clinic Database Backup Guide

## ⚠️ **CRITICAL: Understanding SQLite WAL Mode**

Your database uses **WAL (Write-Ahead Logging)** mode for better concurrent access. This creates **3 files**:

```
database/
├── BSK.db         ← Main database (updated periodically via "checkpoint")
├── BSK.db-wal     ← Write-Ahead Log (contains RECENT uncommitted data)
└── BSK.db-shm     ← Shared Memory (coordination file)
```

### **The Problem You Experienced:**

```
❌ WRONG - What Happened:
1. You copied ONLY BSK.db (old data from last checkpoint)
2. You LOST BSK.db-wal (contained checkups 184-1978)
3. Result: 1795 checkups disappeared!

✅ CORRECT - What You Should Do:
1. Stop the server OR checkpoint first
2. Copy the main BSK.db file (now contains all data)
3. Restore it
```

---

## 📋 **Backup Methods**

### **Method 1: Using Built-in Google Drive Backup (RECOMMENDED) ✅**

This is now **COMPLETELY SAFE** - backs up ALL 3 database files!

**Steps:**
1. Open **Server Dashboard**
2. Click **"Sao lưu CSDL lên Drive"** button
3. Watch the progress:
   - "Đang kiểm tra các tệp cơ sở dữ liệu..."
   - Shows size of each file (BSK.db, BSK.db-wal, BSK.db-shm)
   - "Đang tải lên tất cả các tệp database..."
   - "✅ Sao lưu hoàn tất!"
4. ✅ Done! All files backed up to a timestamped folder.

**What happens behind the scenes:**
```java
// server.jar automatically does this:
1. Checks all 3 files: BSK.db, BSK.db-wal, BSK.db-shm
2. Creates timestamped folder: "_Database_Backups/Backup_2025-01-15_14-30-00/"
3. Uploads ALL files to that folder
4. No data loss possible!
```

**Google Drive Structure:**
```
BSK_Clinic_Patient_Files/
└── _Database_Backups/
    ├── Backup_2025-01-15_14-30-00/
    │   ├── BSK.db         (main database)
    │   ├── BSK.db-wal     (uncommitted changes)
    │   └── BSK.db-shm     (shared memory)
    └── Backup_2025-01-15_09-15-00/
        └── ... (older backup)
```

---

### **Method 2: Manual Backup with PowerShell Script**

**For manual backups when server is running:**

```powershell
# Step 1: Checkpoint the database
.\checkpoint_database.ps1

# Step 2: Copy the database
Copy-Item database\BSK.db "D:\Backups\BSK_backup_$(Get-Date -Format 'yyyy-MM-dd_HH-mm-ss').db"
```

**For manual backups when server is stopped:**

```powershell
# Just copy all 3 files
Copy-Item database\BSK.db* "D:\Backups\"
```

---

### **Method 3: Using SQLite Command Line**

```bash
# Option A: Checkpoint first (if server is running)
sqlite3 database/BSK.db "PRAGMA wal_checkpoint(TRUNCATE);"

# Option B: Create a proper backup
sqlite3 database/BSK.db ".backup 'backup.db'"
```

---

## 🔄 **Restoring from Backup**

### **Method 1: Restore Complete Backup from Google Drive**

**Steps:**
1. **Stop the server** completely
2. Download the **entire backup folder** from Google Drive
3. Navigate to `_Database_Backups/Backup_YYYY-MM-DD_HH-MM-SS/`
4. Download all 3 files:
   - `BSK.db`
   - `BSK.db-wal` (if present)
   - `BSK.db-shm` (if present)
5. **Delete or backup your current database files**:
   ```powershell
   # Backup current database
   Move-Item database\BSK.db* backup_old\
   ```
6. **Copy downloaded files** to `database/` folder
7. **Restart the server**
8. ✅ **Done!** All data restored including uncommitted transactions

### **Method 2: Restore Only Main DB (Data Loss Risk!)**

⚠️ **WARNING:** Only use this if no WAL file exists in the backup!

**Steps:**
1. Stop the server
2. Download only `BSK.db` from backup
3. Copy to `database/` folder
4. Restart server
5. ⚠️ Any data in the WAL file at backup time is **lost**

---

## 🔧 **When to Checkpoint**

SQLite automatically checkpoints when:
- WAL file reaches **1000 pages** (~4MB)
- Database is closed gracefully
- Connection pool is shut down

**Manual checkpoint is needed when:**
- ⚠️ Server has been running for days without restart
- ⚠️ You need to backup while server is running
- ⚠️ WAL file is very large (>10MB)

**Check WAL status:**
```powershell
.\checkpoint_database.ps1 -CheckOnly
```

---

## 🚨 **Emergency: Recovering from Bad Backup**

If you already uploaded a backup that's missing data:

### **Option 1: Check if WAL files still exist at hospital**

```powershell
# At the hospital server, check:
Get-ChildItem database\BSK.db*

# If BSK.db-wal exists:
sqlite3 database\BSK.db "PRAGMA wal_checkpoint(TRUNCATE);"
# Now backup this file - it has all data!
```

### **Option 2: Restore from older backup + replay transactions**

If WAL is gone, data is **permanently lost**. You'll need:
1. Use the latest backup before modification
2. Manually re-enter lost data
3. Use filesystem folder IDs to identify missing checkups:

```powershell
# Find checkup folders
Get-ChildItem image\checkup_media -Directory | 
    Where-Object { $_.Name -match '^\d+$' } | 
    Sort-Object { [int]$_.Name }

# Compare with database
sqlite3 database\BSK.db "SELECT checkup_id FROM Checkup ORDER BY checkup_id"
```

---

## 📊 **Best Practices**

### ✅ **DO:**
- ✅ Use the built-in Google Drive backup button
- ✅ Stop the server before manual file copies
- ✅ Test your backups regularly
- ✅ Keep multiple backup versions
- ✅ Backup before schema changes
- ✅ Document any manual database modifications

### ❌ **DON'T:**
- ❌ Copy only BSK.db while server is running
- ❌ Modify the database on your machine while hospital uses it
- ❌ Ignore WAL files during backup
- ❌ Restore old backups without checking timestamps
- ❌ Edit production database without testing first

---

## 🛠️ **Schema Change Workflow**

When you need to add columns or modify the database:

```
CORRECT Process:
──────────────────────────────────────────────────
1. STOP the hospital server completely
2. Backup using: .\checkpoint_database.ps1
3. Download the backup
4. Test changes on your machine
5. Create migration script (ALTER TABLE commands)
6. Send ONLY the migration script (not the whole DB)
7. Run migration script at hospital
8. Restart server
9. Verify everything works

NEVER:
──────────────────────────────────────────────────
❌ Download → Modify → Upload entire database
   (This WILL cause data loss!)
```

### **Migration Script Example:**

```sql
-- migration_add_doctor_columns.sql
BEGIN TRANSACTION;

-- Add new column
ALTER TABLE Doctor ADD COLUMN specialty TEXT;

-- Update existing data if needed
UPDATE Doctor SET specialty = 'General' WHERE specialty IS NULL;

COMMIT;
```

**Apply migration:**
```bash
sqlite3 database/BSK.db < migration_add_doctor_columns.sql
```

---

## 🔍 **Troubleshooting**

### **Problem: WAL file is huge (>100MB)**

```powershell
# Force checkpoint
sqlite3 database\BSK.db "PRAGMA wal_checkpoint(TRUNCATE);"

# If it fails, connections are open:
# Stop the server, then retry
```

### **Problem: "database is locked"**

- Server is running → stop it first
- Old connections → restart server
- Check: `database\BSK.db-shm` (if exists, connections are active)

### **Problem: Backup seems too small**

```powershell
# Check WAL size
Get-Item database\BSK.db-wal

# If WAL is large, you forgot to checkpoint!
.\checkpoint_database.ps1
```

---

## 📞 **Support**

If you encounter data loss:
1. **DON'T PANIC**
2. **DON'T overwrite any files**
3. Check if old backups exist on Google Drive
4. Contact: [Your Support Contact]

---

## 📚 **Further Reading**

- [SQLite WAL Mode Documentation](https://www.sqlite.org/wal.html)
- [SQLite Backup Documentation](https://www.sqlite.org/backup.html)
- [Understanding WAL Checkpoints](https://www.sqlite.org/wal.html#ckpt)

---

**Last Updated:** $(Get-Date -Format 'yyyy-MM-dd')  
**Version:** 1.0  
**Author:** BSK Development Team

