package com.aistudio.smartcostpro.app.data.repository

import com.aistudio.smartcostpro.app.data.local.Mappers.toDomain
import com.aistudio.smartcostpro.app.data.local.Mappers.toEntity
import com.aistudio.smartcostpro.app.data.local.dao.AssociationDao
import com.aistudio.smartcostpro.app.data.local.dao.StudentDao
import com.aistudio.smartcostpro.app.domain.model.Association
import com.aistudio.smartcostpro.app.domain.model.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val studentDao: StudentDao,
    private val expenseRepository: ExpenseRepository
) {
    fun observeAll(): Flow<List<Student>> =
        studentDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun getAll(): List<Student> = studentDao.getAll().map { it.toDomain() }

    suspend fun getById(id: String): Student? = studentDao.getById(id)?.toDomain()

    suspend fun getByNationalId(nationalId: String): Student? =
        studentDao.getByNationalId(nationalId)?.toDomain()

    suspend fun upsert(student: Student) {
        val now = System.currentTimeMillis()
        studentDao.upsert(student.copy(updatedAt = now).toEntity())
    }

    suspend fun update(student: Student) {
        studentDao.update(student.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    /**
     * Deletes a student AND their associated student-typed expenses.
     *
     * **Spec compliance:**
     *  - Deleting a student must not leave orphan expense rows.
     *  - Caller must confirm before invoking.
     */
    suspend fun deleteById(id: String) {
        expenseRepository.deleteStudentExpenses(id)
        studentDao.deleteById(id)
    }

    suspend fun upsertAll(rows: List<Student>) {
        studentDao.upsertAll(rows.map { it.toEntity() })
    }

    suspend fun deleteAll() = studentDao.deleteAll()
}

@Singleton
class AssociationRepository @Inject constructor(
    private val associationDao: AssociationDao
) {
    fun observeAll(): Flow<List<Association>> =
        associationDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun getAll(): List<Association> = associationDao.getAll().map { it.toDomain() }

    suspend fun getById(id: String): Association? = associationDao.getById(id)?.toDomain()

    suspend fun upsert(association: Association) {
        val now = System.currentTimeMillis()
        associationDao.upsert(association.copy(updatedAt = now).toEntity())
    }

    suspend fun update(association: Association) {
        associationDao.update(association.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteById(id: String) = associationDao.deleteById(id)

    suspend fun upsertAll(rows: List<Association>) {
        associationDao.upsertAll(rows.map { it.toEntity() })
    }

    suspend fun deleteAll() = associationDao.deleteAll()
}
