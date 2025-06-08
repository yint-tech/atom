package cn.iinti.katom.base.storage

import java.io.File

/**
 * 存储一个文件
 *
 * @param path 逻辑路径
 * @param file 文件
 */
interface IStorage {
    fun store(path: String, file: File)

    /**
     * 删除一个文件，在更新文件的时候，为了保证文件一致性，我们会同步删除非当前存储方案中的文件，而不是执行双写方案
     * 不执行双写的原因是本地存储理论上不是足够可靠的
     *
     * @param path 逻辑路径
     */
    fun delete(path: String)

    /**
     * 判断文件是否存在
     *
     * @param path 逻辑路径
     * @return 是否存在
     */
    fun exist(path: String): Boolean

    /**
     * 根据路径获取对应文件
     *
     * @param path            逻辑路径
     * @param destinationFile 获取文件之后存储到目标文件中
     */
    fun getFile(path: String, destinationFile: File?): File

    /**
     * 将资源转换为一个http链接，存储不需要考虑链接有效期，理论上应该是每当需要资源的时候重新计算一次链接。
     * 如果当前存储存在有效期功能，并用户希望做无有效期拦截的URL，那么需要在存储方案供应商那里手动放开权限
     *
     * @param path 逻辑路径
     * @return 一个http地址
     */
    fun genUrl(path: String): String

    /**
     * 判断当前存储方案是否可用，本地存储方案一直可用，也将作为默认存储放哪。
     * oss方案依赖方案供应商，需要管理员在后台配置了对应的参数才可用
     *
     * @return 是否可用
     */
    fun available(): Boolean

    /**
     * 当前存储方案的id
     *
     * @return id字符串
     */
    fun id(): String
}