# DuckyChat Protocol

## Packet format

```
Format:
| 8bit | 8bit   | 8bit  | 8bit  | 32bit |        | 16bit |          | 16bit |
| CMD  | STATUS | FLAGS | PARAM | SIZE  | DATA ( | Size0 | SubData0 | Size1 | SubData1 | ..... ) |
|      MANDATORY                        | [ Optional ]                                           |

CMD:
AUTH =               0x00
CHALLENGE =          0x01
EXIT =               0x02
MSG =                0x03
MSG_ACK =            0x04
LIST_USERS =         0x05
FILE_INIT =          0x06
FILE_DATA =          0x07
FILE_ACK =           0x08
FILE_OVER =          0x09
HELLO =              0xFF

STATUS:
SUCCESS = 0x00
FAILURE = 0x01

FLAGS: | CHIF | LEGACY USER LIST | RESERVED | RESERVED | RESERVED | RESERVED | RESERVED | RESERVED |

                  16 bit          16 bit
KEY FORMAT: | SIZE EXPONENT | SIZE MODULUS | EXPONENT | MODULUS |

Data that can be ciphered: Filename,Message,FileData
```

## Packet types

```
EXAMPLE:
| CMD        | STATUS          | FLAGS    | PARAM | DATA SIZE | DATA ...... |
| ---------- | --------------- | -------- | ----- | --------- | ----------- |
| AUTH       | SUCCESS/FAILURE | 00000000 | 0     | DATA SIZE | DATA (SIZE USERNAME | USERNAME) |
| AUTH       | SUCCESS/FAILURE | 00000000 | 0     | DATA SIZE | DATA (SIZE USERNAME | USERNAME | SIZE PUBKEY | PUBKEY) |
| CHALLENGE  | SUCCESS         | 00000000 | 0     | DATA SIZE | DATA (SIZE PASSWORD | PASSWORD) |
| EXIT       | SUCCESS         | 00000000 | 0     | 0         | 
| SEND_MSG   | SUCCESS         | 00000000 | 0     | DATA SIZE | DATA (SIZE FROM | FROM | SIZE MSG | MSG | [ SIZE TARGET | TARGET ]) |
| LIST_USERS | SUCCESS         | 00000000 | 0     | 0         |
| LIST_USERS | SUCCESS         | 00000000 | 0     | DATA SIZE | DATA (SIZE P1 | P1 | SIZE P2 | P2) |    -> PX : Structure UserProperties
| FILE_INIT  | SUCCESS/FAILURE | 10000000 | NONCE | DATA SIZE | DATA (SIZE FROM | FROM | SIZE FILENAME | FILENAME | 4 | FILE SIZE | [ SIZE TARGET | TARGET ]) |
| FILE_DATA  | SUCCESS/FAILURE | 10000000 | NONCE | DATA SIZE | DATA (SIZE FROM | FROM | SIZE FILE_CHUNK | FILE_CHUNK | [ SIZE TARGET | TARGET ]) |
| FILE_ACK   | SUCCESS/FAILURE | 10000000 | NONCE |
| FILE_OVER  | SUCCESS/FAILURE | 10000000 | NONCE | 0         | DATA (SIZE FROM | FROM | [ SIZE TARGET | TARGET ]) |

Structures:
UserProperties:
|     8 bit     |          |    8 bit    |
| SIZE USERNAME | USERNAME | CLIENT TYPE |

  Client type:
    LIGHT = 0x00
    HEAVY = 0x01
```

## Protocol specifications

In `RFC.pdf` file
