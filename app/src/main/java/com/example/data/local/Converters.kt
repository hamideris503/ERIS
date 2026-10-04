package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.AssetType
import com.example.domain.model.BankSupportStatus
import com.example.domain.model.CategoryType
import com.example.domain.model.CheckStatus
import com.example.domain.model.CheckType
import com.example.domain.model.SmsStatus
import com.example.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.OTHER)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType =
        runCatching { CategoryType.valueOf(value) }.getOrDefault(CategoryType.EXPENSE)

    @TypeConverter
    fun fromCheckType(value: CheckType): String = value.name

    @TypeConverter
    fun toCheckType(value: String): CheckType =
        runCatching { CheckType.valueOf(value) }.getOrDefault(CheckType.ISSUED)

    @TypeConverter
    fun fromCheckStatus(value: CheckStatus): String = value.name

    @TypeConverter
    fun toCheckStatus(value: String): CheckStatus =
        runCatching { CheckStatus.valueOf(value) }.getOrDefault(CheckStatus.PENDING)

    @TypeConverter
    fun fromAssetType(value: AssetType): String = value.name

    @TypeConverter
    fun toAssetType(value: String): AssetType =
        runCatching { AssetType.valueOf(value) }.getOrDefault(AssetType.BANK)

    @TypeConverter
    fun fromSmsStatus(value: SmsStatus): String = value.name

    @TypeConverter
    fun toSmsStatus(value: String): SmsStatus =
        runCatching { SmsStatus.valueOf(value) }.getOrDefault(SmsStatus.PENDING)

    @TypeConverter
    fun fromBankSupportStatus(value: BankSupportStatus): String = value.name

    @TypeConverter
    fun toBankSupportStatus(value: String): BankSupportStatus =
        runCatching { BankSupportStatus.valueOf(value) }.getOrDefault(BankSupportStatus.UNKNOWN)
}
