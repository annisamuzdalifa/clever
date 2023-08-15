package com.pool.services

import net.corda.core.internal.openHttpConnection
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import nonapi.io.github.classgraph.json.JSONDeserializer
import nonapi.io.github.classgraph.json.JSONSerializer
import nonapi.io.github.classgraph.json.JSONUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*

@CordaService
class OracleService(serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
    fun getRateOf(origin: String, dest: String) : Map<Pair<String, String>, Double> {
        val url = URL("https://free.currconv.com/api/v7/convert?q=${origin}_${dest},${dest}_${origin}&compact=ultra&apiKey=ec276f85b72195102433")
        val con = url.openHttpConnection()

        con.requestMethod = "GET"

        val response = StringBuilder()
        try {
            val br = BufferedReader(InputStreamReader(con.inputStream))
            br.lines().forEach {
                response.append(it.trim())
            }
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }

        val randPercent = Random().nextDouble() * 0.01

        val maps = response.filterNot { it == '{' || it == '}' }.split(',').associate { it ->
            val (left, right) = it.split(":")
            val (from, to) = left.filterNot { it =='"' }.split("_")
            Pair(from, to) to right.toDouble()*(1-randPercent)
//            Pair(from, to) to right.toDouble()
        }
        return maps
    }

    fun updateAllCurrency(listCurrency: Set<String>) : Map<Pair<String, String>, Double> {
        val rate = mutableMapOf<Pair<String, String>, Double>()
        createPair(listCurrency.toList()).forEach {
            rate += getRateOf(it.first, it.second)
        }

        return rate
    }

    private fun createPair(listCurrency: List<String>) : List<Pair<String, String>> {
        val lists = mutableListOf<Pair<String, String>>()
        var i = 0
        while(i < listCurrency.size) {
            var j = i+1
            while (j <listCurrency.size) {
                lists.add(Pair(listCurrency[i], listCurrency[j]))
                j++
            }
            i++
        }
        return lists
    }
}