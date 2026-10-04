package com.example.parser

import com.example.parser.banks.MellatParser
import com.example.parser.banks.MelliParser

object ParserRegistry {

    private val parsers = mutableListOf<BankSmsParser>()

    init {
        // Register verified bank parsers
        register(MellatParser())
        register(MelliParser())
    }

    fun register(parser: BankSmsParser) {
        if (parsers.none { it.bankId == parser.bankId }) {
            parsers.add(parser)
        }
    }

    fun getParserFor(message: NormalizedSms): BankSmsParser? {
        return parsers.firstOrNull { it.canParse(message) }
    }

    fun parse(message: NormalizedSms): ParseResult {
        val parser = getParserFor(message)
            ?: return ParseResult.Unsupported("بانک یا الگوی این پیامک شناسایی نشد")
        return parser.parse(message)
    }
}
