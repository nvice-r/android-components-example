package com.example.components.architecture.nvice.data.repository

import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.example.components.architecture.nvice.dao.UserDao
import com.example.components.architecture.nvice.data.datasource.DataSourceFactory
import com.example.components.architecture.nvice.data.preference.AppSettingsPreference
import com.example.components.architecture.nvice.model.User
import com.example.components.architecture.nvice.util.scheduler.DefaultScheduler
import com.example.components.architecture.nvice.ui.user.UserDataSourceFactory
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
        private val userDao: UserDao,
        private val userDataSourceFactory: UserDataSourceFactory,
        private val appSettingsPreference: AppSettingsPreference
) {

    companion object {
        private const val PREFETCH_DISTANCE = 10
        private const val DATABASE_PAGE_SIZE = 20
    }

    var result = MutableLiveData<PagedList<User>>()

    private val pagedListConfig = PagedList.Config.Builder()
            .setPageSize(DATABASE_PAGE_SIZE)
            .setPrefetchDistance(PREFETCH_DISTANCE)
            .setEnablePlaceholders(true)
            .build()

    init {
        getUserList().observeForever {
            result.postValue(it)
        }
        Timber.i(getPreferenceStatus().toString())
    }

    private fun getPreferenceStatus() = appSettingsPreference.has()

    private fun getUserList() = LivePagedListBuilder(userDataSourceFactory, pagedListConfig).build()

    fun getUserStatusList() = userDao.selectAllUserStatus()

    fun getUserPagedListByStatus(statusList: List<Int>) {
        userDataSourceFactory.searchByStatus(statusList)
        result.value?.dataSource?.invalidate()
    }

    suspend fun getUserById(id: Int) = userDao.findByIdAsync(id)

    fun getLatestUserId() = userDao.selectLatestId()

    fun addUser(user: User?) = DefaultScheduler.AsyncScheduler.execute { user?.let { userDao.insert(it) } }

    fun updateUser(user: User?) = DefaultScheduler.AsyncScheduler.execute { user?.let { userDao.update(it) } }

    fun deleteUser(user: User) = DefaultScheduler.AsyncScheduler.execute { userDao.delete(user) }

    fun toggleSort() {
        userDataSourceFactory.setDescendingOrder(userDataSourceFactory.queryOrderSortType == DataSourceFactory.SortType.ASC)
        result.value?.dataSource?.invalidate()
    }

    fun searchUser(queue: String) {
        userDataSourceFactory.searchByREGEX(queue)
        result.value?.dataSource?.invalidate()
    }
}