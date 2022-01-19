/* formats.h
 *
 */

/*
    NNGS - The No Name Go Server
    Copyright (C) 1995  Erik Van Riper (geek@imageek.york.cuny.edu)
    and John Tromp (tromp@daisy.uwaterloo.ca/tromp@cwi.nl)

    Adapted from:
    fics - An internet chess server.
    Copyright (C) 1993  Richard V. Nash

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/
#ifndef FORMATS_H
#define FORMATS_H

#include "language.h"

	/* dont forget to update this one after adding #defines! */
#define FORMAT_CNT 900

	/* AvK
	** Sorry about this, at the moment I consider
	** keeping this stuff in one file more important
	** than keeping code out of header files.
        ** This should be changed, once things are stable.
	** (NB: it all depends on WANT_TO_DEFINE beeing set by one includer).
	*/
void language_init(void);

#if WANT_TO_DEFINE
static const char * formats_english[FORMAT_CNT]= {NULL,};
static const char * formats_chinese[FORMAT_CNT]= {NULL,};
static const char * formats_german[FORMAT_CNT]= {NULL,};
#define formats_default formats_english
#define ENG(n,s) formats_english[(n)] = (s);
#define CHI(n,s) formats_chinese[(n)] = (s);
#define GER(n,s) formats_german[(n)] = (s);
void language_init(void)
{
#else /* WANT_TO_DEFINE */
#define ENG(n,s) /*  eng [(n)] = (s) */
#define CHI(n,s) /*  chi [(n)] = (s) */
#define GER(n,s) /*  ger [(n)] = (s) */
#endif /* WANT_TO_DEFINE */

#define FORMAT_COUNT (851+1)
#define FORMAT_empty 0
ENG(FORMAT_empty,"")

/** start insert here */

#define FORMAT_PLEASE_TYPE_qHELP_TEACHqn 1
ENG(FORMAT_PLEASE_TYPE_qHELP_TEACHqn,"Please type \"help teach\"\n")
CHI(FORMAT_PLEASE_TYPE_qHELP_TEACHqn,"�п�J \"help teach\"\n")
GER(FORMAT_PLEASE_TYPE_qHELP_TEACHqn,"Bitte \"help teach\" tippen\n")

#define FORMAT_I_SUGGEST_THAT_YOU_PLAY_s_AGAINST_s_n 2
ENG(FORMAT_I_SUGGEST_THAT_YOU_PLAY_s_AGAINST_s_n,"I suggest that you play %s against %s:\n")
CHI(FORMAT_I_SUGGEST_THAT_YOU_PLAY_s_AGAINST_s_n,"�ګ�ĳ�z��%s�l, �b�P %s �什��:\n")
GER(FORMAT_I_SUGGEST_THAT_YOU_PLAY_s_AGAINST_s_n,"Schlage vor %s gegen %s zu spielen:\n")

#define FORMAT_FOR_19X19_PLAY_AN_EVEN_GAME_AND_SET_KOMI_TO_f_n 3
ENG(FORMAT_FOR_19X19_PLAY_AN_EVEN_GAME_AND_SET_KOMI_TO_f_n,"For 19x19:  Play an even game and set komi to %.1f.\n")
CHI(FORMAT_FOR_19X19_PLAY_AN_EVEN_GAME_AND_SET_KOMI_TO_f_n,"�Q�E��:  �������l, �K�� (komi) �]�� %.1f.\n")
GER(FORMAT_FOR_19X19_PLAY_AN_EVEN_GAME_AND_SET_KOMI_TO_f_n,"Bei 19x19:  Ein 'even game' spielen mit %.1f komi.\n")

#define FORMAT_FOR_19X19_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n 4
ENG(FORMAT_FOR_19X19_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n,"For 19x19:  %s %d handicap stones and set komi to %.1f.\n")
CHI(FORMAT_FOR_19X19_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n,"�Q�E��:  %s %d �l, �K�� (komi) �]�� %.1f.\n")
GER(FORMAT_FOR_19X19_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n,"Bei 19x19:  %s %d handicap Steine und %.1f komi.\n")

#define FORMAT_FOR_19X19_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n 5
ENG(FORMAT_FOR_19X19_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n,"For 19x19:  %s 9 handicap stones and set komi to 0.5\n")
CHI(FORMAT_FOR_19X19_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n,"�Q�E��:  %s 9 �l, �K�� (komi) �]�� 0.5\n")
GER(FORMAT_FOR_19X19_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n,"Bei 19x19:  %s 9 handicap Steine und 0.5 komi\n")

#define FORMAT_FOR_13X13_PLAY_AN_EVEN_GAME_AND_SET_KOMI_TO_f_n 6
ENG(FORMAT_FOR_13X13_PLAY_AN_EVEN_GAME_AND_SET_KOMI_TO_f_n,"For 13x13:  Play an even game and set komi to %.1f.\n")
CHI(FORMAT_FOR_13X13_PLAY_AN_EVEN_GAME_AND_SET_KOMI_TO_f_n,"�Q�T��:  �������l, �K�� (komi) �]�� %.1f.\n")

#define FORMAT_FOR_13X13_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n 7
ENG(FORMAT_FOR_13X13_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n,"For 13x13:  %s %d handicap stones and set komi to %.1f.\n")
CHI(FORMAT_FOR_13X13_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n,"�Q�T��:  %s %d �l, �K�� (komi) �]�� %.1f.\n")

#define FORMAT_FOR_13X13_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n 8
ENG(FORMAT_FOR_13X13_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n,"For 13x13:  %s 9 handicap stones and set komi to 0.5\n")
CHI(FORMAT_FOR_13X13_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n,"�Q�T��:  %s 9 �l, �K�� (komi) �]�� 0.5\n")

#define FORMAT_FOR_9X9_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n 9
ENG(FORMAT_FOR_9X9_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n,"For   9x9:  %s %d handicap stones and set komi to %.1f.\n")
CHI(FORMAT_FOR_9X9_s_d_HANDICAP_STONES_AND_SET_KOMI_TO_f_n,"  �E��:  %s %d �l, �K�� (komi) �]�� %.1f.\n")

#define FORMAT_FOR_9X9_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n 10
ENG(FORMAT_FOR_9X9_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n,"For   9x9:  %s 9 handicap stones and set komi to 0.5\n")
CHI(FORMAT_FOR_9X9_s_9_HANDICAP_STONES_AND_SET_KOMI_TO_0_5n,"  �E��:  %s 9 �l, �K�� (komi) �]�� 0.5\n")

#define FORMAT_MATCH_d_s_ss_VS_s_ss_n 11
ENG(FORMAT_MATCH_d_s_ss_VS_s_ss_n,"{Match %d: %s [%3.3s%s] vs. %s [%3.3s%s] }\n")
CHI(FORMAT_MATCH_d_s_ss_VS_s_ss_n,"{�﫳�}�l %d: %s [%3.3s%s] vs. %s [%3.3s%s] }\n")
GER(FORMAT_MATCH_d_s_ss_VS_s_ss_n,"{Spiel %d: %s [%3.3s%s] gegen %s [%3.3s%s] }\n")

#define FORMAT_IF_YOU_DON_T_LIKE_OUR_SUGGESTION_YOU_CAN_ASK_WHITE_TO_UNDO_HANDICAPS_n 12
ENG(FORMAT_IF_YOU_DON_T_LIKE_OUR_SUGGESTION_YOU_CAN_ASK_WHITE_TO_UNDO_HANDICAPS_n,"If you don't like our suggestion, you can ask white to undo handicaps.\n")
CHI(FORMAT_IF_YOU_DON_T_LIKE_OUR_SUGGESTION_YOU_CAN_ASK_WHITE_TO_UNDO_HANDICAPS_n,"�Y�Q�ϥΨ�L��X�A�i�Хդ�ϥ� undo ���O�A���s�]�w�C\n")
GER(FORMAT_IF_YOU_DON_T_LIKE_OUR_SUGGESTION_YOU_CAN_ASK_WHITE_TO_UNDO_HANDICAPS_n,"Wenn under Vorschlag nicht gefaellt, dann Weiss bitten den Handicap zu annulieren.\n")

#define FORMAT_IF_YOU_DON_T_LIKE_OUR_SUGGESTION_YOU_CAN_UNDO_HANDICAPS_n 13
ENG(FORMAT_IF_YOU_DON_T_LIKE_OUR_SUGGESTION_YOU_CAN_UNDO_HANDICAPS_n,"If you don't like our suggestion, you can undo handicaps.\n")
CHI(FORMAT_IF_YOU_DON_T_LIKE_OUR_SUGGESTION_YOU_CAN_UNDO_HANDICAPS_n,"�Y�Q�ϥΨ�L��X�A�i�ϥ� undo ���O�A���s�]�w�C\n")

#define FORMAT_YOU_HAVE_NO_OFFERS_TO_ACCEPT_ 14
ENG(FORMAT_YOU_HAVE_NO_OFFERS_TO_ACCEPT_,"You have no offers to accept.")
CHI(FORMAT_YOU_HAVE_NO_OFFERS_TO_ACCEPT_,"�S���H��z���X�n�D.")
GER(FORMAT_YOU_HAVE_NO_OFFERS_TO_ACCEPT_,"Es gibt keine Angebote zu akzeptieren.")

#define FORMAT_YOU_HAVE_MORE_THAN_ONE_OFFER_TO_ACCEPT_n 15
ENG(FORMAT_YOU_HAVE_MORE_THAN_ONE_OFFER_TO_ACCEPT_n,"You have more than one offer to accept.\n")
CHI(FORMAT_YOU_HAVE_MORE_THAN_ONE_OFFER_TO_ACCEPT_n,"�O�H��z���n�D����@��.")
GER(FORMAT_YOU_HAVE_MORE_THAN_ONE_OFFER_TO_ACCEPT_n,"Es gibt mehr als ein Angebot zu akzeptieren.\n")

#define FORMAT_USE_qPENDINGq_TO_SEE_THEM_AND_qACCEPT_Nq_TO_CHOOSE_WHICH_ONE_ 16
ENG(FORMAT_USE_qPENDINGq_TO_SEE_THEM_AND_qACCEPT_Nq_TO_CHOOSE_WHICH_ONE_,"Use \"pending\" to see them and \"accept n\" to choose which one.")
CHI(FORMAT_USE_qPENDINGq_TO_SEE_THEM_AND_qACCEPT_Nq_TO_CHOOSE_WHICH_ONE_,"��J \"pending\" �C�X, �٦� \"accept n\" �ӿ�ܨ䤤�@��.")

#define FORMAT_OUT_OF_RANGE_USE_qPENDINGq_TO_SEE_THE_LIST_OF_OFFERS_ 17
ENG(FORMAT_OUT_OF_RANGE_USE_qPENDINGq_TO_SEE_THE_LIST_OF_OFFERS_,"Out of range. Use \"pending\" to see the list of offers.")
CHI(FORMAT_OUT_OF_RANGE_USE_qPENDINGq_TO_SEE_THE_LIST_OF_OFFERS_,"�W�X�d��. ��J \"pending\" �C�X�Ҧ����n�D.")

#define FORMAT_THERE_ARE_NO_PENDING_s_OFFERS_ 18
ENG(FORMAT_THERE_ARE_NO_PENDING_s_OFFERS_,"There are no pending %s offers.")
CHI(FORMAT_THERE_ARE_NO_PENDING_s_OFFERS_,"�S���|���ѨM�� %s �n�D.")

#define FORMAT_THERE_ARE_NO_PENDING_OFFERS_FROM_s_ 19
ENG(FORMAT_THERE_ARE_NO_PENDING_OFFERS_FROM_s_,"There are no pending offers from %s.")
CHI(FORMAT_THERE_ARE_NO_PENDING_OFFERS_FROM_s_,"%s �S���X���󥼸ѨM���n�D.")

#define FORMAT_YOU_HAVE_NO_PENDING_OFFERS_FROM_OTHER_PLAYERS_ 20
ENG(FORMAT_YOU_HAVE_NO_PENDING_OFFERS_FROM_OTHER_PLAYERS_,"You have no pending offers from other players.")
CHI(FORMAT_YOU_HAVE_NO_PENDING_OFFERS_FROM_OTHER_PLAYERS_,"�S���H��z���X�n�D.")

#define FORMAT_YOU_HAVE_MORE_THAN_ONE_PENDING_OFFER_ 21
ENG(FORMAT_YOU_HAVE_MORE_THAN_ONE_PENDING_OFFER_,"You have more than one pending offer.")
CHI(FORMAT_YOU_HAVE_MORE_THAN_ONE_PENDING_OFFER_,"�z�w�V�O�H���X����@�����ШD.")

#define FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_DECLINE_ 22
ENG(FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_DECLINE_,"Please specify which one you wish to decline.")

#define FORMAT_PENDING_WILL_GIVE_YOU_THE_LIST_ 23
ENG(FORMAT_PENDING_WILL_GIVE_YOU_THE_LIST_,"'Pending' will give you the list.")
CHI(FORMAT_PENDING_WILL_GIVE_YOU_THE_LIST_,"��J 'Pending' �i�o��@���M��.")
GER(FORMAT_PENDING_WILL_GIVE_YOU_THE_LIST_,"'Pending' gibt eine Liste.")

#define FORMAT_INVALID_OFFER_NUMBER_MUST_BE_BETWEEN_1_AND_d_ 24
ENG(FORMAT_INVALID_OFFER_NUMBER_MUST_BE_BETWEEN_1_AND_d_,"Invalid offer number. Must be between 1 and %d.")
CHI(FORMAT_INVALID_OFFER_NUMBER_MUST_BE_BETWEEN_1_AND_d_,"�n�D�N�X���s�b. �����b 1 �M %d ����.")
GER(FORMAT_INVALID_OFFER_NUMBER_MUST_BE_BETWEEN_1_AND_d_,"Ungueltiges Angebot.  Muss zwischen 1 und %d liegen.")

#define FORMAT_d_OFFERS_DECLINED 25
ENG(FORMAT_d_OFFERS_DECLINED,"%d offers declined")
CHI(FORMAT_d_OFFERS_DECLINED,"%d ���n�D�w�ڵ�")
GER(FORMAT_d_OFFERS_DECLINED,"%d Angebote abgelehnt")

#define FORMAT_YOU_HAVE_NO_PENDING_OFFERS_TO_OTHER_PLAYERS_ 26
ENG(FORMAT_YOU_HAVE_NO_PENDING_OFFERS_TO_OTHER_PLAYERS_,"You have no pending offers to other players.")
CHI(FORMAT_YOU_HAVE_NO_PENDING_OFFERS_TO_OTHER_PLAYERS_,"�èS���V��L�H���X����ШD.")
GER(FORMAT_YOU_HAVE_NO_PENDING_OFFERS_TO_OTHER_PLAYERS_,"Es liegen keine match Angebote an andere Spieler vor.")

#define FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_WITHDRAW_ 27
ENG(FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_WITHDRAW_,"Please specify which one you wish to withdraw.")
CHI(FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_WITHDRAW_,"�Ы��w�z�ҷQ���������@��.")
GER(FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_WITHDRAW_,"Bitte angeben welches Angebot zurueckzuziehen ist.")

#define FORMAT_THERE_ARE_NO_OFFERS_PENDING_TO_OTHER_PLAYERS_n 28
ENG(FORMAT_THERE_ARE_NO_OFFERS_PENDING_TO_OTHER_PLAYERS_n,"There are no offers pending TO other players.\n")
CHI(FORMAT_THERE_ARE_NO_OFFERS_PENDING_TO_OTHER_PLAYERS_n,"�å��V��L�H���X����ШD.\n")
GER(FORMAT_THERE_ARE_NO_OFFERS_PENDING_TO_OTHER_PLAYERS_n,"Es liegen keine Angebote AN andere Spieler vor.\n")

#define FORMAT_OFFERS_TO_OTHER_PLAYERS_n 29
ENG(FORMAT_OFFERS_TO_OTHER_PLAYERS_n,"Offers TO other players:\n")
CHI(FORMAT_OFFERS_TO_OTHER_PLAYERS_n,"�V�L�H�w���X���ШD:\n")
GER(FORMAT_OFFERS_TO_OTHER_PLAYERS_n,"Angebote AN andere Spieler:\n")

#define FORMAT_THERE_ARE_NO_OFFERS_PENDING_FROM_OTHER_PLAYERS_n 30
ENG(FORMAT_THERE_ARE_NO_OFFERS_PENDING_FROM_OTHER_PLAYERS_n,"There are no offers pending FROM other players.\n")
CHI(FORMAT_THERE_ARE_NO_OFFERS_PENDING_FROM_OTHER_PLAYERS_n,"�S���H�V�z���X�ШD.\n")
GER(FORMAT_THERE_ARE_NO_OFFERS_PENDING_FROM_OTHER_PLAYERS_n,"Es liegen keine Angebote VON anderen Spielern vor.\n")

#define FORMAT_OFFERS_FROM_OTHER_PLAYERS_n 31
ENG(FORMAT_OFFERS_FROM_OTHER_PLAYERS_n,"Offers FROM other players:\n")
CHI(FORMAT_OFFERS_FROM_OTHER_PLAYERS_n,"�L�H��z���X���ШD:\n")
GER(FORMAT_OFFERS_FROM_OTHER_PLAYERS_n,"Angebote VON anderen Spielern:\n")

#define FORMAT_IF_YOU_WISH_TO_ACCEPT_ANY_OF_THESE_OFFERS_TYPE_ACCEPT_N_ 32
ENG(FORMAT_IF_YOU_WISH_TO_ACCEPT_ANY_OF_THESE_OFFERS_TYPE_ACCEPT_N_,"If you wish to accept any of these offers type 'accept n'")
CHI(FORMAT_IF_YOU_WISH_TO_ACCEPT_ANY_OF_THESE_OFFERS_TYPE_ACCEPT_N_,"�p�G�z�Q��������@���ШD, �п�J 'accept n'")

#define FORMAT_OR_JUST_ACCEPT_IF_THERE_IS_ONLY_ONE_OFFER_n 33
ENG(FORMAT_OR_JUST_ACCEPT_IF_THERE_IS_ONLY_ONE_OFFER_n,"or just 'accept' if there is only one offer.\n")
CHI(FORMAT_OR_JUST_ACCEPT_IF_THERE_IS_ONLY_ONE_OFFER_n,"�Ϊ̦p�G�`�@�u���@��, ��J 'accept' �Y�i.\n")

#define FORMAT_GAMES_CURRENTLY_BEING_OBSERVED_ 34
ENG(FORMAT_GAMES_CURRENTLY_BEING_OBSERVED_,"Games currently being observed: ")
CHI(FORMAT_GAMES_CURRENTLY_BEING_OBSERVED_,"�ثe���b�[�ݪ��ѧ�: ")
GER(FORMAT_GAMES_CURRENTLY_BEING_OBSERVED_,"Spiele die observiert werden: ")

#define FORMAT_GAMES_CURRENTLY_BEING_OBSERVED_NONE_n 35
ENG(FORMAT_GAMES_CURRENTLY_BEING_OBSERVED_NONE_n,"Games currently being observed:  None.\n")
CHI(FORMAT_GAMES_CURRENTLY_BEING_OBSERVED_NONE_n,"�ثe���b�[�ݪ��ѧ�: �L.\n")
GER(FORMAT_GAMES_CURRENTLY_BEING_OBSERVED_NONE_n,"Spiele die observiert werden: Keine.\n")

#define FORMAT_TYPE_qHELP_OVERVIEWq_FOR_A_LIST_OF_LGS_GENERAL_INFORMATION_FILES_n 36
ENG(FORMAT_TYPE_qHELP_OVERVIEWq_FOR_A_LIST_OF_LGS_GENERAL_INFORMATION_FILES_n,"[Type \"help overview\" for a list of LGS general information files.]\n")
CHI(FORMAT_TYPE_qHELP_OVERVIEWq_FOR_A_LIST_OF_LGS_GENERAL_INFORMATION_FILES_n,"[��J \"help overview\" �C�X LGS �Ҧ��@�몺��T�ɮ�.]\n")

#define FORMAT_MATCHES_s 37
ENG(FORMAT_MATCHES_s,"Matches: %s")
CHI(FORMAT_MATCHES_s,"�i�ର: %s")
GER(FORMAT_MATCHES_s,"Spiele: %s")

#define FORMAT_HELPFILE_s_COULD_NOT_BE_FOUND_ 38
ENG(FORMAT_HELPFILE_s_COULD_NOT_BE_FOUND_,"Helpfile %s could not be found!\n")
CHI(FORMAT_HELPFILE_s_COULD_NOT_BE_FOUND_,"�䤣�� %s ���D�U��!\n")
GER(FORMAT_HELPFILE_s_COULD_NOT_BE_FOUND_,"Hilfsdokument %s war unauffindbar!\n")

#define FORMAT_WELCOME_TO_NNGS_PAIR_GO_n 39
ENG(FORMAT_WELCOME_TO_NNGS_PAIR_GO_n,"Welcome to NNGS' Pair Go!\n")
GER(FORMAT_WELCOME_TO_NNGS_PAIR_GO_n,"Wilkommen bei NNGS' Pair Go!\n")

#define FORMAT_YOU_AND_s_ARE_THE_WHITE_TEAM_n 40
ENG(FORMAT_YOU_AND_s_ARE_THE_WHITE_TEAM_n,"You, and %s are the White team.\n")

#define FORMAT_MATCHED_PLAYER_s_AT_ADDRESS_sn 41
ENG(FORMAT_MATCHED_PLAYER_s_AT_ADDRESS_sn,"Matched player: %-10s at address %s\n")
CHI(FORMAT_MATCHED_PLAYER_s_AT_ADDRESS_sn,"�ŦX���Ѥ�: %-10s �H�c��}�� %s\n")

#define FORMAT_GAME_d_s_s_d_d_d_VS_s_d_d_d_n 42
ENG(FORMAT_GAME_d_s_s_d_d_d_VS_s_d_d_d_n,"Game %d %s: %s (%d %d %d) vs %s (%d %d %d)\n")

#define FORMAT_GAME_IS_TITLED_d_sn 43
ENG(FORMAT_GAME_IS_TITLED_d_sn,"Game is titled: %d %s\n")
GER(FORMAT_GAME_IS_TITLED_d_sn,"Spiel Titel: %d %s\n")

#define FORMAT_n 44
ENG(FORMAT_n,"\n")
GER(FORMAT_n,"\n")

#define FORMAT_d_c_sn 45
ENG(FORMAT_d_c_sn,"%3d(%c): %s\n")
GER(FORMAT_d_c_sn,"%3d(%c): %s\n")

#define FORMAT_UPDATING_OFFER_ALREADY_MADE_TO_qsq_n 46
ENG(FORMAT_UPDATING_OFFER_ALREADY_MADE_TO_qsq_n,"Updating offer already made to \"%s\".\n")
CHI(FORMAT_UPDATING_OFFER_ALREADY_MADE_TO_qsq_n,"���� \"%s\" �ܽФU�Ѫ��]�w.\n")

#define FORMAT_DECLINING_RULE_OFFER_FROM_s_AND_OFFERING_NEW_RULE_n 47
ENG(FORMAT_DECLINING_RULE_OFFER_FROM_s_AND_OFFERING_NEW_RULE_n,"Declining rule offer from %s and offering new rule.\n")

#define FORMAT_s_DECLINES_YOUR_RULE_OFFER_AND_OFFERS_A_NEW_RULE_n 48
ENG(FORMAT_s_DECLINES_YOUR_RULE_OFFER_AND_OFFERS_A_NEW_RULE_n,"%s declines your rule offer, and offers a new rule:\n")

#define FORMAT_UPDATING_RULE_OFFER_TO_ 49
ENG(FORMAT_UPDATING_RULE_OFFER_TO_,"Updating rule offer to: ")

#define FORMAT_s_UPDATES_THE_RULE_OFFER_n 50
ENG(FORMAT_s_UPDATES_THE_RULE_OFFER_n,"%s updates the rule offer.\n")

#define FORMAT_OFFERING_JAPANESE_RULE_TO_s 51
ENG(FORMAT_OFFERING_JAPANESE_RULE_TO_s,"Offering japanese rule to %s")

#define FORMAT_OFFERING_THE_ING_S_RULE_TO_s 52
ENG(FORMAT_OFFERING_THE_ING_S_RULE_TO_s,"Offering the Ing's rule to %s")

#define FORMAT_s_OFFERS_JAPANESE_RULE_n 53
ENG(FORMAT_s_OFFERS_JAPANESE_RULE_n,"%s offers japanese rule.\n")

#define FORMAT_s_OFFERS_THE_ING_S_RULE_n 54
ENG(FORMAT_s_OFFERS_THE_ING_S_RULE_n,"%s offers the Ing's rule.\n")

#define FORMAT_USE_qRULE_dq_TO_ACCEPT_OR_qDECLINE_sq_TO_RESPOND_n 55
ENG(FORMAT_USE_qRULE_dq_TO_ACCEPT_OR_qDECLINE_sq_TO_RESPOND_n,"Use \"rule %d\" to accept, or \"decline %s\" to respond.\n")

#define FORMAT_SORRY_THAT_IS_A_PRIVATE_GAME_ 56
ENG(FORMAT_SORRY_THAT_IS_A_PRIVATE_GAME_,"Sorry, that is a private game.")
GER(FORMAT_SORRY_THAT_IS_A_PRIVATE_GAME_,"Dieses Spiel ist als 'privat' markiert.")

#define FORMAT_IT_IS_NOT_YOUR_MOVE_ 57
ENG(FORMAT_IT_IS_NOT_YOUR_MOVE_,"It is not your move.")
CHI(FORMAT_IT_IS_NOT_YOUR_MOVE_,"�{�b�����⸨�l.")
GER(FORMAT_IT_IS_NOT_YOUR_MOVE_,"Sie sind nicht dran.")

#define FORMAT_s_UNDID_THE_LAST_MOVE_s_n 58
ENG(FORMAT_s_UNDID_THE_LAST_MOVE_s_n,"%s undid the last move (%s).\n")
GER(FORMAT_s_UNDID_THE_LAST_MOVE_s_n,"%s hat ihren letzten Spielzug zurueck genommen(%s).\n")

#define FORMAT_WHITE_NAME_RK_BLACK_NAME_RK_MOVE_SIZE_H_KOMI_BY_FR_ 59
ENG(FORMAT_WHITE_NAME_RK_BLACK_NAME_RK_MOVE_SIZE_H_KOMI_BY_FR_,"[##]  white name [ rk ]      black name [ rk ] (Move size H Komi BY FR) (###)")
CHI(FORMAT_WHITE_NAME_RK_BLACK_NAME_RK_MOVE_SIZE_H_KOMI_BY_FR_,"[##]  ���մѪ�   [����]      ���´Ѫ�   [����] (Move size H Komi BY FR) (###)")

#define FORMAT_d_s_ss_VS_s_ss_d_d_d_f_d_cc_d_ 60
ENG(FORMAT_d_s_ss_VS_s_ss_d_d_d_f_d_cc_d_,"[%2d] %11.11s [%3.3s%s] vs. %11.11s [%3.3s%s] (%3d   %2d  %d  %2.1f %2d %c%c) (%3d)")

#define FORMAT_REMOVING_GAME_d_FROM_OBSERVATION_LIST_n 61
ENG(FORMAT_REMOVING_GAME_d_FROM_OBSERVATION_LIST_n,"Removing game %d from observation list.\n")
CHI(FORMAT_REMOVING_GAME_d_FROM_OBSERVATION_LIST_n,"�N�ѧ� %d �q�[�ݲM�椤����.\n")

#define FORMAT_ADDING_GAME_TO_OBSERVATION_LIST_n 62
ENG(FORMAT_ADDING_GAME_TO_OBSERVATION_LIST_n,"Adding game to observation list.\n")
CHI(FORMAT_ADDING_GAME_TO_OBSERVATION_LIST_n,"�����[�J�[�ݲM�椤.\n")

#define FORMAT_d_c_s 63
ENG(FORMAT_d_c_s,"%3d(%c): %s")
GER(FORMAT_d_c_s,"%3d(%c): %s")

#define FORMAT_YOU_CANNOT_OBSERVE_A_GAME_THAT_YOU_ARE_PLAYING_n 64
ENG(FORMAT_YOU_CANNOT_OBSERVE_A_GAME_THAT_YOU_ARE_PLAYING_n,"You cannot observe a game that you are playing.\n")
CHI(FORMAT_YOU_CANNOT_OBSERVE_A_GAME_THAT_YOU_ARE_PLAYING_n,"�z�����[�ݦۤv�ҤU���ѧ�.\n")

#define FORMAT_GAME_INFO_sn 65
ENG(FORMAT_GAME_INFO_sn,"Game Info: %s\n")
GER(FORMAT_GAME_INFO_sn,"Spiel Info: %s\n")

#define FORMAT_EVENT_INFO_sn 66
ENG(FORMAT_EVENT_INFO_sn,"Event Info: %s\n")
GER(FORMAT_EVENT_INFO_sn,"Event Info: %s\n")

#define FORMAT_WHITE_PLAYER_s_ssn 67
ENG(FORMAT_WHITE_PLAYER_s_ssn,"White Player: %s %3.3s%s\n")

#define FORMAT_BLACK_PLAYER_s_ssn 68
ENG(FORMAT_BLACK_PLAYER_s_ssn,"Black Player: %s %3.3s%s\n")

#define FORMAT_STORED_GAMES_FOR_s_n 69
ENG(FORMAT_STORED_GAMES_FOR_s_n,"Stored games for %s:\n")

#define FORMAT_COMPLETED_GAMES_FOR_s_n 70
ENG(FORMAT_COMPLETED_GAMES_FOR_s_n,"Completed games for %s:\n")
GER(FORMAT_COMPLETED_GAMES_FOR_s_n,"Gespielte Spiele fuer %s:\n")

#define FORMAT_FOUND_d_COMPLETED_GAMES_n 71
ENG(FORMAT_FOUND_d_COMPLETED_GAMES_n,"Found %d completed games.\n")
GER(FORMAT_FOUND_d_COMPLETED_GAMES_n,"%d gespielte Spiele gefunden.\n")

#define FORMAT_THE_PLAYER_s_IS_NOW_IN_BYO_YOMI_n 72
ENG(FORMAT_THE_PLAYER_s_IS_NOW_IN_BYO_YOMI_n,"The player %s is now in byo-yomi.")
GER(FORMAT_THE_PLAYER_s_IS_NOW_IN_BYO_YOMI_n,"Der Spieler %s ist jetzt in byo-yomi.")

#define FORMAT_YOU_HAVE_d_STONES_AND_d_MINUTESn 73
ENG(FORMAT_YOU_HAVE_d_STONES_AND_d_MINUTESn,"You have %d stones and %d minutes.\n")
GER(FORMAT_YOU_HAVE_d_STONES_AND_d_MINUTESn,"Sie haben %d Steine und %d minuten.\n")

#define FORMAT_THERE_IS_NO_PLAYER_BY_THAT_NAME_ 74
ENG(FORMAT_THERE_IS_NO_PLAYER_BY_THAT_NAME_,"There is no player by that name.")
GER(FORMAT_THERE_IS_NO_PLAYER_BY_THAT_NAME_,"Es existiert kein Spieler under diesem Namen.")

#define FORMAT_YOU_MUST_BE_ONE_OF_THE_TWO_PLAYERS_TO_SRESIGN_ 75
ENG(FORMAT_YOU_MUST_BE_ONE_OF_THE_TWO_PLAYERS_TO_SRESIGN_,"You must be one of the two players to sresign.")
GER(FORMAT_YOU_MUST_BE_ONE_OF_THE_TWO_PLAYERS_TO_SRESIGN_,"Sie muesse einer von zwei spielern sein die 'sresign'en wollen.")

#define FORMAT_YOU_CAN_T_SRESIGN_FOR_A_PLAYER_WHOSE_ADMIN_LEVEL_IS_HIGHER_THAN_YOURS_ 76
ENG(FORMAT_YOU_CAN_T_SRESIGN_FOR_A_PLAYER_WHOSE_ADMIN_LEVEL_IS_HIGHER_THAN_YOURS_,"You can't sresign for a player whose admin level is higher than yours.")

#define FORMAT_THERE_IS_NO_STORED_GAME_s_VS_s_HMMMMM_n 77
ENG(FORMAT_THERE_IS_NO_STORED_GAME_s_VS_s_HMMMMM_n,"There is no stored game %s vs. %s (Hmmmmm)\n")

#define FORMAT_THERE_IS_NO_PLAYER_MATCHING_THAT_NAME_n 78
ENG(FORMAT_THERE_IS_NO_PLAYER_MATCHING_THAT_NAME_n,"There is no player matching that name.\n")
CHI(FORMAT_THERE_IS_NO_PLAYER_MATCHING_THAT_NAME_n,"�䤣��ŦX���W�r���ϥΪ̡C\n")
GER(FORMAT_THERE_IS_NO_PLAYER_MATCHING_THAT_NAME_n,"Es existiert kein Spieler under diesem Namen.")

#define FORMAT_THERE_IS_NO_MORE_n 79
ENG(FORMAT_THERE_IS_NO_MORE_n,"There is no more.\n")
CHI(FORMAT_THERE_IS_NO_MORE_n,"�w�g�S�o\n")
GER(FORMAT_THERE_IS_NO_MORE_n,"Gibt Nix Mehr.\n")

#define FORMAT_TYPE_qNEXTq_OR_TO_SEE_NEXT_PAGE_n 80
ENG(FORMAT_TYPE_qNEXTq_OR_TO_SEE_NEXT_PAGE_n,"Type \"next\" or `  to see next page.\n")
CHI(FORMAT_TYPE_qNEXTq_OR_TO_SEE_NEXT_PAGE_n,"��J \"next\" �� `  �ݤU�@��.\n")
GER(FORMAT_TYPE_qNEXTq_OR_TO_SEE_NEXT_PAGE_n,"Bitte \"next\" oder ` tippen um die naechste Seite zu sehen.\n")

#define FORMAT_YOU_CAN_T_CHALLENGE_WHILE_YOU_ARE_PLAYING_A_GAME_ 81
ENG(FORMAT_YOU_CAN_T_CHALLENGE_WHILE_YOU_ARE_PLAYING_A_GAME_,"You can't challenge while you are playing a game.")
CHI(FORMAT_YOU_CAN_T_CHALLENGE_WHILE_YOU_ARE_PLAYING_A_GAME_,"�z����b�U�Ѫ��ɭԦV�O�H�D��.")

#define FORMAT_YOU_ARE_ALREADY_PLAYING_A_GAME_ 82
ENG(FORMAT_YOU_ARE_ALREADY_PLAYING_A_GAME_,"You are already playing a game.")
CHI(FORMAT_YOU_ARE_ALREADY_PLAYING_A_GAME_,"�z�w�g�b�U�Ѥ�.")

#define FORMAT_YOU_ARE_NOT_PLAYING_A_GAME_ 83
ENG(FORMAT_YOU_ARE_NOT_PLAYING_A_GAME_,"You are not playing a game.")
CHI(FORMAT_YOU_ARE_NOT_PLAYING_A_GAME_,"�z�ëD���b�U��.")
GER(FORMAT_YOU_ARE_NOT_PLAYING_A_GAME_,"Sie spielen kein Spiel.")

#define FORMAT_s_HAS_INCREASED_YOUR_BONUS_BY_d_n 84

#define FORMAT_s_HAS_DECREASED_YOUR_BONUS_BY_d_n 85

#define FORMAT_BONUS_dn 86

#define FORMAT_s_HAS_JOINED_CHANNEL_d_n 87
ENG(FORMAT_s_HAS_JOINED_CHANNEL_d_n,"%s has joined channel %d.\n")
CHI(FORMAT_s_HAS_JOINED_CHANNEL_d_n,"%s �[�J�F�W�D %d.\n")
GER(FORMAT_s_HAS_JOINED_CHANNEL_d_n,"%s ist Kanal %d beigetreten.\n")

#define FORMAT_s_HAS_LEFT_CHANNEL_d_n 88
ENG(FORMAT_s_HAS_LEFT_CHANNEL_d_n,"%s has left channel %d.\n")
CHI(FORMAT_s_HAS_LEFT_CHANNEL_d_n,"%s ���}�F�W�D %d.\n")
GER(FORMAT_s_HAS_LEFT_CHANNEL_d_n,"%s hat Kanal %d verlassen.\n")

#define FORMAT_YOU_ARE_NOT_ON_CHANNEL_dn 89
ENG(FORMAT_YOU_ARE_NOT_ON_CHANNEL_dn,"You are not on channel %d\n")
CHI(FORMAT_YOU_ARE_NOT_ON_CHANNEL_dn,"�z�å����}�W�D %d\n")
GER(FORMAT_YOU_ARE_NOT_ON_CHANNEL_dn,"Sie sind nicht in Kanal %d\n")

#define FORMAT_s_IS_ALREADY_ON_CHANNEL_dn 90
ENG(FORMAT_s_IS_ALREADY_ON_CHANNEL_dn,"%s is already on channel %d\n")
CHI(FORMAT_s_IS_ALREADY_ON_CHANNEL_dn,"%s �w�g���}�F�W�D %d\n")
GER(FORMAT_s_IS_ALREADY_ON_CHANNEL_dn,"%s ist schon in Kanal %d\n")

#define FORMAT_s_INVITES_YOU_TO_CHANNEL_dn 91
ENG(FORMAT_s_INVITES_YOU_TO_CHANNEL_dn,"%s invites you to channel %d.\n")
CHI(FORMAT_s_INVITES_YOU_TO_CHANNEL_dn,"%s �ܽбz�[�J�W�D %d\n")
GER(FORMAT_s_INVITES_YOU_TO_CHANNEL_dn,"%s laedt sie zu Kanal %d ein.\n")

#define FORMAT_INVITED_s_TO_CHANNEL_d 92
ENG(FORMAT_INVITED_s_TO_CHANNEL_d,"Invited %s to channel %d.")
CHI(FORMAT_INVITED_s_TO_CHANNEL_d,"�ܽ� %s �[�J�W�D %d")
GER(FORMAT_INVITED_s_TO_CHANNEL_d,"%s zu Kanal %d eingeladen.")

#define FORMAT_NO_PREVIOUS_CHANNEL_ 93
ENG(FORMAT_NO_PREVIOUS_CHANNEL_,"No previous channel.")
CHI(FORMAT_NO_PREVIOUS_CHANNEL_,"�z�W�^�S�����}�����W�D.")
GER(FORMAT_NO_PREVIOUS_CHANNEL_,"Kein vorheriger Kanal.")

#define FORMAT_INVALID_CHANNEL_NUMBER_ 94
ENG(FORMAT_INVALID_CHANNEL_NUMBER_,"Invalid channel number.")
CHI(FORMAT_INVALID_CHANNEL_NUMBER_,"���W�D���X���s�b.")
GER(FORMAT_INVALID_CHANNEL_NUMBER_,"Ungueltige Kanal Nummer.")

#define FORMAT_THE_LOWEST_CHANNEL_NUMBER_IS_1_ 95
ENG(FORMAT_THE_LOWEST_CHANNEL_NUMBER_IS_1_,"The lowest channel number is 1.")
CHI(FORMAT_THE_LOWEST_CHANNEL_NUMBER_IS_1_,"�̤p���W�D���X�� 1.")

#define FORMAT_THE_MAXIMUM_CHANNEL_NUMBER_IS_d_ 96
ENG(FORMAT_THE_MAXIMUM_CHANNEL_NUMBER_IS_d_,"The maximum channel number is %d.")
CHI(FORMAT_THE_MAXIMUM_CHANNEL_NUMBER_IS_d_,"�̤j���W�D���X�� %d.")

#define FORMAT_THE_USERS_OF_CHANNEL_d_WOULD_PREFER_YOU_TO_BE_IN_THAT_CHANNEL_BEFOREn 97
ENG(FORMAT_THE_USERS_OF_CHANNEL_d_WOULD_PREFER_YOU_TO_BE_IN_THAT_CHANNEL_BEFOREn,"The users of channel %d would prefer you to be in that channel before\n")
CHI(FORMAT_THE_USERS_OF_CHANNEL_d_WOULD_PREFER_YOU_TO_BE_IN_THAT_CHANNEL_BEFOREn,"�W�D %d �̪��ϥΪ̧Ʊ�z���[�J�L�̪��W�D, �A���.")

#define FORMAT_SPEAKING_IN_IT_SEE_qHELP_CHANNELq 98
ENG(FORMAT_SPEAKING_IN_IT_SEE_qHELP_CHANNELq,"speaking in it.  See \"help channel\"")
CHI(FORMAT_SPEAKING_IN_IT_SEE_qHELP_CHANNELq,"�аѷ� \"help channel\"")

#define FORMAT_CHANNEL_d_TURNED_OFF_n 100
ENG(FORMAT_CHANNEL_d_TURNED_OFF_n,"Channel %d turned off.\n")
CHI(FORMAT_CHANNEL_d_TURNED_OFF_n,"�����W�D %d .\n")
GER(FORMAT_CHANNEL_d_TURNED_OFF_n,"Kanal %d ausgeschaltet.\n")

#define FORMAT_CHANNEL_d_TURNED_ON_n 101
ENG(FORMAT_CHANNEL_d_TURNED_ON_n,"Channel %d turned on.\n")
CHI(FORMAT_CHANNEL_d_TURNED_ON_n,"���}�W�D %d .\n")
GER(FORMAT_CHANNEL_d_TURNED_ON_n,"Kanal %d angeschaltet.\n")

#define FORMAT_CHANNEL_d_TITLE_sn 102
ENG(FORMAT_CHANNEL_d_TITLE_sn,"Channel %d Title:%s\n")
CHI(FORMAT_CHANNEL_d_TITLE_sn,"�W�D %d �D�D:%s\n")
GER(FORMAT_CHANNEL_d_TITLE_sn,"Kanal %d Titel:%s\n")

#define FORMAT_CHANNEL_d_IS_ALREADY_FULL_n 103
ENG(FORMAT_CHANNEL_d_IS_ALREADY_FULL_n,"Channel %d is already full.\n")
CHI(FORMAT_CHANNEL_d_IS_ALREADY_FULL_n,"�W�D %d �Ⱥ���.\n")
GER(FORMAT_CHANNEL_d_IS_ALREADY_FULL_n,"Kanal %d ist schon voll.\n")

#define FORMAT_MAXIMUM_CHANNEL_NUMBER_EXCEEDED_n 104
ENG(FORMAT_MAXIMUM_CHANNEL_NUMBER_EXCEEDED_n,"Maximum channel number exceeded.\n")
CHI(FORMAT_MAXIMUM_CHANNEL_NUMBER_EXCEEDED_n,"�Ҧ��W�D���w���}.\n")

#define FORMAT_INVALID_CHANNEL_n 105
ENG(FORMAT_INVALID_CHANNEL_n,"Invalid Channel.\n")
CHI(FORMAT_INVALID_CHANNEL_n,"���s�b���W�D.\n")

#define FORMAT_SORRY_THAT_CHANNEL_IS_LOCKED_n 106
ENG(FORMAT_SORRY_THAT_CHANNEL_IS_LOCKED_n,"Sorry, that channel is locked.\n")
CHI(FORMAT_SORRY_THAT_CHANNEL_IS_LOCKED_n,"��p, ���W�D�w���.\n")

#define FORMAT_SORRY_THAT_CHANNEL_IS_CLOSED_n 107
ENG(FORMAT_SORRY_THAT_CHANNEL_IS_CLOSED_n,"Sorry, that channel is closed.\n")
CHI(FORMAT_SORRY_THAT_CHANNEL_IS_CLOSED_n,"��p, ���W�D�w����.\n")

#define FORMAT_s_HAS_UNLOCKED_CHANNEL_dn 108
ENG(FORMAT_s_HAS_UNLOCKED_CHANNEL_dn,"%s has unlocked channel %d\n")
CHI(FORMAT_s_HAS_UNLOCKED_CHANNEL_dn,"%s �N�W�D %d �}��\n")

#define FORMAT_s_HAS_DO_NOT_DISTURBED_CHANNEL_dn 109
ENG(FORMAT_s_HAS_DO_NOT_DISTURBED_CHANNEL_dn,"%s has do not disturbed channel %d\n")
CHI(FORMAT_s_HAS_DO_NOT_DISTURBED_CHANNEL_dn,"%s �N�W�D %d �]���~�H�L�k�Ǹ�\n")

#define FORMAT_s_HAS_REMOVED_THE_DO_NOT_DISTURB_ON_CHANNEL_dn 110
ENG(FORMAT_s_HAS_REMOVED_THE_DO_NOT_DISTURB_ON_CHANNEL_dn,"%s has removed the do not disturb on channel %d\n")
CHI(FORMAT_s_HAS_REMOVED_THE_DO_NOT_DISTURB_ON_CHANNEL_dn,"%s �N�W�D %d �]�����` (�~�H�i�H����)\n")

#define FORMAT_s_HAS_CHANGED_THE_TITLE_OF_CHANNEL_d_TO_sn 111
ENG(FORMAT_s_HAS_CHANGED_THE_TITLE_OF_CHANNEL_d_TO_sn,"%s has changed the title of channel %d to:%s\n")
CHI(FORMAT_s_HAS_CHANGED_THE_TITLE_OF_CHANNEL_d_TO_sn,"%s �N�W�D %d ���D�D�]��:%s\n")

#define FORMAT_TWO_PARAMETER_INCHANNEL_IS_NOT_IMPLEMENTED_ 112
ENG(FORMAT_TWO_PARAMETER_INCHANNEL_IS_NOT_IMPLEMENTED_,"Two parameter inchannel is not implemented.")
CHI(FORMAT_TWO_PARAMETER_INCHANNEL_IS_NOT_IMPLEMENTED_,"���Ѽ��W�D�|���i��.")

#define FORMAT_CHANNEL_d_sss_s 113
ENG(FORMAT_CHANNEL_d_sss_s,"Channel %d: (%s%s%s) %s")
CHI(FORMAT_CHANNEL_d_sss_s,"�W�D %d: (%s%s%s) %s")
GER(FORMAT_CHANNEL_d_sss_s,"Kanal %d: (%s%s%s) %s")

#define FORMAT_NO_CHANNELS_IN_USE_n 114
ENG(FORMAT_NO_CHANNELS_IN_USE_n,"No channels in use.\n")
CHI(FORMAT_NO_CHANNELS_IN_USE_n,"�S���W�D���}.\n")

#define FORMAT_CHANNEL_NOT_IN_USE_n 115
ENG(FORMAT_CHANNEL_NOT_IN_USE_n,"Channel not in use.\n")
CHI(FORMAT_CHANNEL_NOT_IN_USE_n,"�W�D�|�����}.\n")

#define FORMAT_ILLEGAL_CHARACTER_IN_COMMAND_s_n 116
ENG(FORMAT_ILLEGAL_CHARACTER_IN_COMMAND_s_n,"Illegal character in command %s.\n")
CHI(FORMAT_ILLEGAL_CHARACTER_IN_COMMAND_s_n,"���O %s �������X�k���r��.\n")

#define FORMAT_AMBIGUOUS_COMMAND_MATCHES_ 117
ENG(FORMAT_AMBIGUOUS_COMMAND_MATCHES_,"Ambiguous command. Matches:")
CHI(FORMAT_AMBIGUOUS_COMMAND_MATCHES_,"�R�O�����T. �i�ର:")

#define FORMAT_USAGE_nss 118
ENG(FORMAT_USAGE_nss,"Usage: \n%s%s")
CHI(FORMAT_USAGE_nss,"�ϥΤ覡: \n%s%s")

#define FORMAT_HELP_s_Z_n 119
ENG(FORMAT_HELP_s_Z_n,"=P%N 'help %s' (S,]'9>c*:1T-z.\n")
CHI(FORMAT_HELP_s_Z_n,"See 'help %s' for a complete description.\n")
GER(FORMAT_HELP_s_Z_n,"\nBitte 'help %s' fuer eine komplette Beschreibung konsultieren.\n")

#define FORMAT_BAD_VALUE_GIVEN_FOR_VARIABLE_s_ 120
ENG(FORMAT_BAD_VALUE_GIVEN_FOR_VARIABLE_s_,"Bad value given for variable %s.")
CHI(FORMAT_BAD_VALUE_GIVEN_FOR_VARIABLE_s_,"�ܼ� %s ���Ѽƭȿ��~.")

#define FORMAT_UNKNOWN_VALUE_FOR_TOGGLING_ 121
ENG(FORMAT_UNKNOWN_VALUE_FOR_TOGGLING_,"Unknown value for toggling.")
CHI(FORMAT_UNKNOWN_VALUE_FOR_TOGGLING_,"�S���o�إi��������.")

#define FORMAT_AMBIGUOUS_VARIABLE_NAME_s_ 122
ENG(FORMAT_AMBIGUOUS_VARIABLE_NAME_s_,"Ambiguous variable name %s.")
CHI(FORMAT_AMBIGUOUS_VARIABLE_NAME_s_,"�����T���ܼ� %s.")

#define FORMAT_s_UNKNOWN_COMMAND_n 123
ENG(FORMAT_s_UNKNOWN_COMMAND_n,"%s: Unknown command.\n")
CHI(FORMAT_s_UNKNOWN_COMMAND_n,"%s: �L���R�O.\n")
GER(FORMAT_s_UNKNOWN_COMMAND_n,"%s: Unbekannter Befehl.\n")

#define FORMAT_MATCH_COMMAND_TRANSFERS_TO_RMATCH_s_d_dn 124
ENG(FORMAT_MATCH_COMMAND_TRANSFERS_TO_RMATCH_s_d_dn,"Match command transfers to: rmatch %s %d %d\n")
CHI(FORMAT_MATCH_COMMAND_TRANSFERS_TO_RMATCH_s_d_dn,"�什���O�۰��ഫ��: rmatch %s %d %d\n")

#define FORMAT_NO_HELP_AVAILABLE_ON_s_n 125
ENG(FORMAT_NO_HELP_AVAILABLE_ON_s_n,"No help available on %s.\n")
CHI(FORMAT_NO_HELP_AVAILABLE_ON_s_n,"�|�����Ѧ��� %s ������.\n")
GER(FORMAT_NO_HELP_AVAILABLE_ON_s_n,"Keine Hilfe fuer %s vorhanden.\n")

#define FORMAT_THERE_IS_NO_SUCH_GAME_n 126
ENG(FORMAT_THERE_IS_NO_SUCH_GAME_n,"There is no such game.\n")
CHI(FORMAT_THERE_IS_NO_SUCH_GAME_n,"�S������.\n")

#define FORMAT_M_W_ 127
ENG(FORMAT_M_W_,"Player name is too long")
GER(FORMAT_M_W_,"Spieler Name zu lang")

#define FORMAT_M_W_U 128
ENG(FORMAT_M_W_U,"Player name is too short")
GER(FORMAT_M_W_U,"Spieler Name ist zu kurz")

#define FORMAT_GUEST_O_B_H_U_C 129
ENG(FORMAT_GUEST_O_B_H_U_C,"guest 3o-S1b89$#/`59$H5y%U!C")
CHI(FORMAT_GUEST_O_B_H_U_C,"It would not be nice to register guest.")

#define FORMAT_M_W_T_X_K_R_U_A_Z_A_Z_R_ 130
ENG(FORMAT_M_W_T_X_K_R_U_A_Z_A_Z_R_,"Illegal characters in player name. Only A-Z a-z allowed.")

#define FORMAT_O_B_s_W_G_Q_U_F_ 131
ENG(FORMAT_O_B_s_W_G_Q_U_F_,"A player by the name %s is already registered.")

#define FORMAT_X_K_Q_L_L_n 132
ENG(FORMAT_X_K_Q_L_L_n,"Invalid email address!\n")

#define FORMAT_INVALID_LANGUAGE_NUMBER_SEE_qHELP_LANGUAGEq_n 133
ENG(FORMAT_INVALID_LANGUAGE_NUMBER_SEE_qHELP_LANGUAGEq_n,"Invalid language number. See \"help language\".\n")

#define FORMAT_BAD_INDEX_NUMBER_n 134
ENG(FORMAT_BAD_INDEX_NUMBER_n,"Bad index number!\n")
CHI(FORMAT_BAD_INDEX_NUMBER_n,"�S���o�Ӹ��X!\n")

#define FORMAT_NO_MORE_INFO_n 135
ENG(FORMAT_NO_MORE_INFO_n,"No more info.\n")
CHI(FORMAT_NO_MORE_INFO_n,"�w�L��h�T��.\n")

#define FORMAT_INTERNAL_ERROR_COULDN_T_SEND_NEWS_FILE_n 136
ENG(FORMAT_INTERNAL_ERROR_COULDN_T_SEND_NEWS_FILE_n,"Internal error - couldn't send news file!\n")
CHI(FORMAT_INTERNAL_ERROR_COULDN_T_SEND_NEWS_FILE_n,"�������~ - �L�k�ǰe�ɮ�, �Чi���t�κ޲z��!\n")

#define FORMAT_NO_SUCH_LIST_n 137
ENG(FORMAT_NO_SUCH_LIST_n,"No such list!\n")
CHI(FORMAT_NO_SUCH_LIST_n,"�S���o���W��!\n")

#define FORMAT_INVALID_RANK_FLAG_n 138
ENG(FORMAT_INVALID_RANK_FLAG_n,"Invalid rank flag.\n")
CHI(FORMAT_INVALID_RANK_FLAG_n,"�����T���ѤO����.\n")

#define FORMAT_s_IS_ALREADY_IN_THE_s_LIST_n 139
ENG(FORMAT_s_IS_ALREADY_IN_THE_s_LIST_n,"[%s] is already in the %s list.\n")
CHI(FORMAT_s_IS_ALREADY_IN_THE_s_LIST_n,"[%s] �w�b %s �W�椤.\n")

#define FORMAT_s_ADDED_TO_THE_s_LIST_n 140
ENG(FORMAT_s_ADDED_TO_THE_s_LIST_n,"[%s] added to the %s list.\n")
CHI(FORMAT_s_ADDED_TO_THE_s_LIST_n,"[%s] �[�J %s �W��.\n")

#define FORMAT_YOU_ARE_NOT_ALLOWED_TO_EDIT_THIS_LIST_n 141
ENG(FORMAT_YOU_ARE_NOT_ALLOWED_TO_EDIT_THIS_LIST_n,"You are not allowed to edit this list!\n")
CHI(FORMAT_YOU_ARE_NOT_ALLOWED_TO_EDIT_THIS_LIST_n,"�z�L�k�ק�o���W��!\n")

#define FORMAT_s_IS_NOT_IN_THE_s_LIST_n 142
ENG(FORMAT_s_IS_NOT_IN_THE_s_LIST_n,"[%s] is not in the %s list.\n")
CHI(FORMAT_s_IS_NOT_IN_THE_s_LIST_n,"[%s] ���b %s �W�椤.\n")

#define FORMAT_s_HAS_BEEN_REMOVED_FROM_THE_s_LIST_n 143
ENG(FORMAT_s_HAS_BEEN_REMOVED_FROM_THE_s_LIST_n,"[%s] has been removed from the %s list.\n")
CHI(FORMAT_s_HAS_BEEN_REMOVED_FROM_THE_s_LIST_n,"�N [%s] �q %s �W�椤����.\n")

#define FORMAT_LISTS_n 144
ENG(FORMAT_LISTS_n,"Lists:\n")
CHI(FORMAT_LISTS_n,"�W��:\n")

#define FORMAT_s_IS_sn 145
ENG(FORMAT_s_IS_sn,"%-20s is %s\n")
CHI(FORMAT_s_IS_sn,"%-20s �O %s\n")

#define FORMAT_YOU_ARE_NOT_ALLOWED_TO_SEE_THIS_LIST_ 146
ENG(FORMAT_YOU_ARE_NOT_ALLOWED_TO_SEE_THIS_LIST_,"You are not allowed to see this list!")
CHI(FORMAT_YOU_ARE_NOT_ALLOWED_TO_SEE_THIS_LIST_,"�z�L�k�ק�o���W��!")

#define FORMAT_ERROR_COULDN_T_FIND_THE_LIST_NAMED_s_n 147
ENG(FORMAT_ERROR_COULDN_T_FIND_THE_LIST_NAMED_s_n,"Error: couldn't find the list named %s!\n")
CHI(FORMAT_ERROR_COULDN_T_FIND_THE_LIST_NAMED_s_n,"���~: ���s�b�W�� %s ���W��!\n")

#define FORMAT_THE_s_LIST_d_NAMES_ 148
ENG(FORMAT_THE_s_LIST_d_NAMES_,"-- The %s list: %d names --")
CHI(FORMAT_THE_s_LIST_d_NAMES_,"-- %s �W��: %d �ӤH --")

#define FORMAT_YOU_HAVE_d_MESSAGES_TYPE_qMESSAGESq_TO_DISPLAY_THEMn 149
ENG(FORMAT_YOU_HAVE_d_MESSAGES_TYPE_qMESSAGESq_TO_DISPLAY_THEMn,"You have %d messages.  Type \"messages\" to display them\n")
CHI(FORMAT_YOU_HAVE_d_MESSAGES_TYPE_qMESSAGESq_TO_DISPLAY_THEMn,"�z�� %d �D�T��. ��J \"messages\" �iŪ��.\n")

#define FORMAT_YOUR_MESSAGE_CONTAINS_SOME_UNPRINTABLE_CHARACTER_S_ 150
ENG(FORMAT_YOUR_MESSAGE_CONTAINS_SOME_UNPRINTABLE_CHARACTER_S_,"Your message contains some unprintable character(s).")
CHI(FORMAT_YOUR_MESSAGE_CONTAINS_SOME_UNPRINTABLE_CHARACTER_S_,"�z�ҳۥs�����e�]�t�F�L�k��ܪ��r��.")

#define FORMAT_PLAYER_s_IS_CURRENTLY_INVOLVED_IN_A_TOURNEMENT_MATCH_n 151
ENG(FORMAT_PLAYER_s_IS_CURRENTLY_INVOLVED_IN_A_TOURNEMENT_MATCH_n,"Player %s is currently involved in a tournement match.\n")
CHI(FORMAT_PLAYER_s_IS_CURRENTLY_INVOLVED_IN_A_TOURNEMENT_MATCH_n,"%s ���b�i��`�j��.\n")

#define FORMAT_PLEASE_SEND_A_MESSAGE_INSTEAD_ 152
ENG(FORMAT_PLEASE_SEND_A_MESSAGE_INSTEAD_,"Please send a message instead.")
CHI(FORMAT_PLEASE_SEND_A_MESSAGE_INSTEAD_,"�ХH message �N�����.")
GER(FORMAT_PLEASE_SEND_A_MESSAGE_INSTEAD_,"Bitte eine Botschaft (message) schicken.")

#define FORMAT_YOU_ARE_NOT_REGISTERED_AND_CANNOT_SEND_MESSAGES_ 153
ENG(FORMAT_YOU_ARE_NOT_REGISTERED_AND_CANNOT_SEND_MESSAGES_,"You are not registered and cannot send messages.")
CHI(FORMAT_YOU_ARE_NOT_REGISTERED_AND_CANNOT_SEND_MESSAGES_,"�z�|�����U, �]������ǰe�T�����L�H.")
GER(FORMAT_YOU_ARE_NOT_REGISTERED_AND_CANNOT_SEND_MESSAGES_,"Sie sind nicht registriert und koennen keine Botschaften schicken.")

#define FORMAT_NO_MESSAGE_SENT_ 154
ENG(FORMAT_NO_MESSAGE_SENT_,"No message sent.")
CHI(FORMAT_NO_MESSAGE_SENT_,"�����ǰe�T��.")
GER(FORMAT_NO_MESSAGE_SENT_,"Keine Botschaft geschickt.")

#define FORMAT_COULDN_T_SEND_MESSAGE_TO_s_MESSAGE_BUFFER_FULL_ 155
ENG(FORMAT_COULDN_T_SEND_MESSAGE_TO_s_MESSAGE_BUFFER_FULL_,"Couldn't send message to %s. Message buffer full.")
CHI(FORMAT_COULDN_T_SEND_MESSAGE_TO_s_MESSAGE_BUFFER_FULL_,"�L�k�e�T���� %s. �T���Ȧs�Ŷ��w��.")

#define FORMAT_MESSAGE_SENT_TO_s_ 156
ENG(FORMAT_MESSAGE_SENT_TO_s_,"Message sent to %s.")
CHI(FORMAT_MESSAGE_SENT_TO_s_,"�N�T���ǰe�� %s.")
GER(FORMAT_MESSAGE_SENT_TO_s_,"Botschaft an %s geschickt.")

#define FORMAT_s_JUST_SENT_YOU_A_MESSAGE_n 157
ENG(FORMAT_s_JUST_SENT_YOU_A_MESSAGE_n,"%s just sent you a message.\n")
CHI(FORMAT_s_JUST_SENT_YOU_A_MESSAGE_n,"%s �e�F�@�D�T�����z.\n")

#define FORMAT_YOU_HAVE_NO_MESSAGES_ 158
ENG(FORMAT_YOU_HAVE_NO_MESSAGES_,"You have no messages.")
CHI(FORMAT_YOU_HAVE_NO_MESSAGES_,"�z�S�����󪺰T��.")
GER(FORMAT_YOU_HAVE_NO_MESSAGES_,"Sie haben keine Botschaften.")

#define FORMAT_MESSAGES_CLEARED_ 159
ENG(FORMAT_MESSAGES_CLEARED_,"Messages cleared.")
CHI(FORMAT_MESSAGES_CLEARED_,"�T���M������.")
GER(FORMAT_MESSAGES_CLEARED_,"Botschaften geloescht.")

#define FORMAT_YOU_HAVE_NO_MESSAGES_TO_MAIL_n 160
ENG(FORMAT_YOU_HAVE_NO_MESSAGES_TO_MAIL_n,"You have no messages to mail.\n")
CHI(FORMAT_YOU_HAVE_NO_MESSAGES_TO_MAIL_n,"�z�S���T��, �L�i�H�^.\n")

#define FORMAT_YOUR_MESSAGES_WERE_SENT_TO_sn 161
ENG(FORMAT_YOUR_MESSAGES_WERE_SENT_TO_sn,"Your messages were sent to %s\n")
CHI(FORMAT_YOUR_MESSAGES_WERE_SENT_TO_sn,"�z���T���w�H�� %s\n")

#define FORMAT_PLEASE_TYPE_qERASEq_TO_ERASE_YOUR_MESSAGES_AFTER_READINGn 162
ENG(FORMAT_PLEASE_TYPE_qERASEq_TO_ERASE_YOUR_MESSAGES_AFTER_READINGn,"Please type \"erase\" to erase your messages after reading\n")
CHI(FORMAT_PLEASE_TYPE_qERASEq_TO_ERASE_YOUR_MESSAGES_AFTER_READINGn,"�ЦbŪ�����J \"erase\" �M���z���T��\n")

#define FORMAT_RATINGS_UPDATE_YOU_ARE_s_ss_d_WERE_d_d_RATED_GAMES_n 163
ENG(FORMAT_RATINGS_UPDATE_YOU_ARE_s_ss_d_WERE_d_d_RATED_GAMES_n,"Ratings update: You are %s %s%s (%d (were %d), %d rated games)\n")
CHI(FORMAT_RATINGS_UPDATE_YOU_ARE_s_ss_d_WERE_d_d_RATED_GAMES_n,"�n����s: �z%s�O %s%s (%d (�쬰 %d), ���ħ���: %d ��)\n")

#define FORMAT_YOU_ARE_NOW_REGISTERED_CONFIRMATION_TOGETHER_WITH_PASSWORD_IS_SENT_TO_YOUR 164
ENG(FORMAT_YOU_ARE_NOW_REGISTERED_CONFIRMATION_TOGETHER_WITH_PASSWORD_IS_SENT_TO_YOUR,"You are now registered! Confirmation together with password is sent to your")
CHI(FORMAT_YOU_ARE_NOW_REGISTERED_CONFIRMATION_TOGETHER_WITH_PASSWORD_IS_SENT_TO_YOUR," ���U OK ! �t�Τw�o�X�@�ʽT�{�H, �s�P�b���K�X, �H��:")

#define FORMAT_EMAIL_ADDRESS_sn 165
ENG(FORMAT_EMAIL_ADDRESS_sn,"email address: %s\n")
CHI(FORMAT_EMAIL_ADDRESS_sn,"�z���q�l�l��H�c: %s\n")

#define FORMAT_IF_YOU_DON_T_RECIEVE_THE_LETTER_REPORT_IT_TO_ANY_ADMINISTRATOR_n 166
ENG(FORMAT_IF_YOU_DON_T_RECIEVE_THE_LETTER_REPORT_IT_TO_ANY_ADMINISTRATOR_n,"If you don't recieve the letter, report it to any administrator.\n")
CHI(FORMAT_IF_YOU_DON_T_RECIEVE_THE_LETTER_REPORT_IT_TO_ANY_ADMINISTRATOR_n,"�p�G�S����T�{�H, �ЦV���@��t�κ޲z������, ��U���K�X�ιq�l�H�c��}�C\n")

#define FORMAT_Z_W_B_D_O_W_n 167
CHI(FORMAT_Z_W_B_D_O_W_n,"�z���ܤw�����b�d���O�W.\n")

#define FORMAT_Y_W_s_n 168
ENG(FORMAT_Y_W_s_n,")R(O%N*:;y(%3])w,0%s.\n")
CHI(FORMAT_Y_W_s_n,"Language set to %s.\n")

#define FORMAT_THERE_IS_NO_NEWS_SINCE_YOUR_LAST_LOGIN_s_n 169
ENG(FORMAT_THERE_IS_NO_NEWS_SINCE_YOUR_LAST_LOGIN_s_n,"There is no news since your last login (%s).\n")
CHI(FORMAT_THERE_IS_NO_NEWS_SINCE_YOUR_LAST_LOGIN_s_n,"�q�z�W���W����{�b���S���s���� (%s).\n")

#define FORMAT_YOUR_OPPONENT_TYPED_QUIT_PERHAPS_THEY_HAD_AN_EMERGENCY_n 170
ENG(FORMAT_YOUR_OPPONENT_TYPED_QUIT_PERHAPS_THEY_HAD_AN_EMERGENCY_n,"Your opponent typed quit.  Perhaps they had an emergency.\n")
CHI(FORMAT_YOUR_OPPONENT_TYPED_QUIT_PERHAPS_THEY_HAD_AN_EMERGENCY_n,"�z������J quit �h�X�t��, �]�\\�L (�o) �����Ʊ�.\n")

#define FORMAT_BULLETIN_BOARD_n 171
ENG(FORMAT_BULLETIN_BOARD_n,"    **** BULLETIN BOARD ****\n")
CHI(FORMAT_BULLETIN_BOARD_n,"    ****  ��  ��  ��  ****\n")

#define FORMAT_SORRY_YOUR_GSHOUT_IS_TURNED_OFF_FOR_NOW_ 172
ENG(FORMAT_SORRY_YOUR_GSHOUT_IS_TURNED_OFF_FOR_NOW_,"Sorry, your gshout is turned off for now.  :)")
CHI(FORMAT_SORRY_YOUR_GSHOUT_IS_TURNED_OFF_FOR_NOW_,"��p, �z�{�b����ϥ� gshout.  :)")

#define FORMAT_IT_SORRY_TURNED_OFF_FOR_NOW_ 173
ENG(FORMAT_IT_SORRY_TURNED_OFF_FOR_NOW_,"It: Sorry, turned off for now.  :)")
CHI(FORMAT_IT_SORRY_TURNED_OFF_FOR_NOW_,"��p, �z�{�b����ϥ� it.  :)")

#define FORMAT_GIT_SORRY_TURNED_OFF_FOR_NOW_ 174
ENG(FORMAT_GIT_SORRY_TURNED_OFF_FOR_NOW_,"Git: Sorry, turned off for now.  :)")
CHI(FORMAT_GIT_SORRY_TURNED_OFF_FOR_NOW_,"��p, �z�{�b�L�k�ϥ� git.  :)")

#define FORMAT_SORRY_YOUR_SHOUT_IS_TURNED_OFF_FOR_NOW_ 175
ENG(FORMAT_SORRY_YOUR_SHOUT_IS_TURNED_OFF_FOR_NOW_,"Sorry, your shout is turned off for now  :)")
CHI(FORMAT_SORRY_YOUR_SHOUT_IS_TURNED_OFF_FOR_NOW_,"��p, �z�{�b����ϥ� shout :)")

#define FORMAT_YOU_ARE_OUT_OF_WATER_BALLOONS_GO_WIN_SOME_GAMES_ 176
ENG(FORMAT_YOU_ARE_OUT_OF_WATER_BALLOONS_GO_WIN_SOME_GAMES_,"You are out of water balloons.  Go win some games.  :)")
CHI(FORMAT_YOU_ARE_OUT_OF_WATER_BALLOONS_GO_WIN_SOME_GAMES_,"�z�����y�Χ��F. �֥hĹ�X���^�ӧa.  :)")

#define FORMAT_KIBITZ_SORRY_TURNED_OFF_FOR_NOW_ 177
ENG(FORMAT_KIBITZ_SORRY_TURNED_OFF_FOR_NOW_,"Kibitz: Sorry, turned off for now.  :)")
CHI(FORMAT_KIBITZ_SORRY_TURNED_OFF_FOR_NOW_,"��p, �z�� kibitz �\\��Q�����F.  :)")

#define FORMAT_BEEP_OOPS_ 178
ENG(FORMAT_BEEP_OOPS_,"Beep: oops.")

#define FORMAT_YOU_ARE_ALREADY_A_MEMBER_OF_THE_9X9_LADDER_ 179
ENG(FORMAT_YOU_ARE_ALREADY_A_MEMBER_OF_THE_9X9_LADDER_,"You are already a member of the 9x9 ladder.")
CHI(FORMAT_YOU_ARE_ALREADY_A_MEMBER_OF_THE_9X9_LADDER_,"�z�w���[�J�F�E���ƦW��.")

#define FORMAT_YOU_ARE_AT_POSITION_d_IN_THE_9X9_LADDER_GOOD_LUCK_ 180
ENG(FORMAT_YOU_ARE_AT_POSITION_d_IN_THE_9X9_LADDER_GOOD_LUCK_,"You are at position %d in the 9x9 ladder.  Good Luck!")
CHI(FORMAT_YOU_ARE_AT_POSITION_d_IN_THE_9X9_LADDER_GOOD_LUCK_,"�b�E���ƦW�ɤ��z�ƦW�� %d . ���n�B!")

#define FORMAT_YOU_ARE_ALREADY_A_MEMBER_OF_THE_19X19_LADDER_ 181
ENG(FORMAT_YOU_ARE_ALREADY_A_MEMBER_OF_THE_19X19_LADDER_,"You are already a member of the 19x19 ladder.")
CHI(FORMAT_YOU_ARE_ALREADY_A_MEMBER_OF_THE_19X19_LADDER_,"�z�w���[�J�F�Q�E���ƦW��.")

#define FORMAT_YOU_ARE_AT_POSITION_d_IN_THE_19X19_LADDER_GOOD_LUCK_ 182
ENG(FORMAT_YOU_ARE_AT_POSITION_d_IN_THE_19X19_LADDER_GOOD_LUCK_,"You are at position %d in the 19x19 ladder.  Good Luck!")
CHI(FORMAT_YOU_ARE_AT_POSITION_d_IN_THE_19X19_LADDER_GOOD_LUCK_,"�b�Q�E���ƦW�ɤ��ƦW�� %d . ���n�B!")

#define FORMAT_YOU_ARE_IN_LADDERS_n 183
ENG(FORMAT_YOU_ARE_IN_LADDERS_n,"You are in ladders:\n")
CHI(FORMAT_YOU_ARE_IN_LADDERS_n,"�z�w�[�J���ƦW��:\n")

#define FORMAT_LADDER9_POSITION_dn 184
ENG(FORMAT_LADDER9_POSITION_dn,"Ladder9 position: %d\n")
CHI(FORMAT_LADDER9_POSITION_dn,"�E�����ɱƦW����:  %d\n")

#define FORMAT_LADDER19_POSITION_d 185
ENG(FORMAT_LADDER19_POSITION_d,"Ladder19       Position %d")
CHI(FORMAT_LADDER19_POSITION_d,"19���ƦW��     �z���ƦW %d")

#define FORMAT_YOU_ARE_NOT_A_MEMBER_OF_THE_9X9_LADDER_ 186
ENG(FORMAT_YOU_ARE_NOT_A_MEMBER_OF_THE_9X9_LADDER_,"You are not a member of the 9x9 ladder.")
CHI(FORMAT_YOU_ARE_NOT_A_MEMBER_OF_THE_9X9_LADDER_,"�z�å��[�J�E���ƦW��.")

#define FORMAT_YOU_HAVE_BEEN_REMOVED_FROM_THE_9X9_LADDER_ 187
ENG(FORMAT_YOU_HAVE_BEEN_REMOVED_FROM_THE_9X9_LADDER_,"You have been removed from the 9x9 ladder.")
CHI(FORMAT_YOU_HAVE_BEEN_REMOVED_FROM_THE_9X9_LADDER_,"�z�h�X�F�E���ƦW��.")

#define FORMAT_YOU_ARE_NOT_A_MEMBER_OF_THE_19X19_LADDER_ 188
ENG(FORMAT_YOU_ARE_NOT_A_MEMBER_OF_THE_19X19_LADDER_,"You are not a member of the 19x19 ladder.")
CHI(FORMAT_YOU_ARE_NOT_A_MEMBER_OF_THE_19X19_LADDER_,"�z�å��[�J�Q�E���ƦW��.")

#define FORMAT_YOU_HAVE_BEEN_REMOVED_FROM_THE_19X19_LADDER_ 189
ENG(FORMAT_YOU_HAVE_BEEN_REMOVED_FROM_THE_19X19_LADDER_,"You have been removed from the 19x19 ladder.")
CHI(FORMAT_YOU_HAVE_BEEN_REMOVED_FROM_THE_19X19_LADDER_,"�z�h�X�F�Q�E���ƦW��.")

#define FORMAT_LADDERS_n 190
ENG(FORMAT_LADDERS_n,"Ladders:\n")
CHI(FORMAT_LADDERS_n,"�ƦW��:\n")

#define FORMAT_LADDER9_9X9_LADDERn 191
ENG(FORMAT_LADDER9_9X9_LADDERn,"Ladder9        9x9 Ladder\n")
CHI(FORMAT_LADDER9_9X9_LADDERn,"Ladder9        �E���ƦW��\n")

#define FORMAT_LADDER19_19X19_LADDER 192
ENG(FORMAT_LADDER19_19X19_LADDER,"Ladder19       19x19 Ladder")
CHI(FORMAT_LADDER19_19X19_LADDER,"Ladder19       �Q�E���ƦW��")

#define FORMAT_LADDER9_POSITION_NUMBER_ONEn 193
ENG(FORMAT_LADDER9_POSITION_NUMBER_ONEn,"Ladder9 position: Number One\n")
CHI(FORMAT_LADDER9_POSITION_NUMBER_ONEn,"�E���ѧ��ƦW����:  Number One\n")

#define FORMAT_LADDER19_POSITION_NUMBER_ONEn 194
ENG(FORMAT_LADDER19_POSITION_NUMBER_ONEn,"Ladder19 position: Number One\n")
CHI(FORMAT_LADDER19_POSITION_NUMBER_ONEn,"�Q�E���ѧ��ƦW����:Number One\n")

#define FORMAT_LADDER19_POSITION_dn 195
ENG(FORMAT_LADDER19_POSITION_dn,"Ladder19 position: %d\n")
CHI(FORMAT_LADDER19_POSITION_dn,"�Q�E���ѧ��ƦW����:%d\n")

#define FORMAT_d_J_s_W_s_W_n 196
ENG(FORMAT_d_J_s_W_s_W_n,"There are currently %d players in the %s ladder.  %s #1!\n")
CHI(FORMAT_d_J_s_W_s_W_n,"�@�� %d ��[�J %s ���ƦW��, %s �ƦW�Ĥ@!\n")

#define FORMAT_THIS_CAN_BE_A_LADDER19_RATED_GAME_ 197
ENG(FORMAT_THIS_CAN_BE_A_LADDER19_RATED_GAME_,"This can be a Ladder19 rated game.")
CHI(FORMAT_THIS_CAN_BE_A_LADDER19_RATED_GAME_,"�o�Ӵѧ��i�H�p�J�Q�E���ѧ��ƦW��.")

#define FORMAT_THIS_CAN_BE_A_LADDER9_RATED_GAME_ 198
ENG(FORMAT_THIS_CAN_BE_A_LADDER9_RATED_GAME_,"This can be a Ladder9 rated game.")
CHI(FORMAT_THIS_CAN_BE_A_LADDER9_RATED_GAME_,"�o�Ӵѧ��i�H�p�J�E���ѧ��ƦW��.")

#define FORMAT_TYPE_LADDER_BEFORE_YOUR_FIRST_MOVE_TO_MAKE_IT_RATED_n 199
ENG(FORMAT_TYPE_LADDER_BEFORE_YOUR_FIRST_MOVE_TO_MAKE_IT_RATED_n,"Type 'ladder' BEFORE your first move to make it rated.\n")
CHI(FORMAT_TYPE_LADDER_BEFORE_YOUR_FIRST_MOVE_TO_MAKE_IT_RATED_n,"�b�A�̤U�Ĥ@��Ѥ��e��J 'ladder' �h�o�Ӵѧ��N�p�J�E���ѧ��ƦW�ɿn��.\n")

#define FORMAT_SORRY_YOU_CANNOT_JOIN_A_LADDER_WHILE_PLAYING_A_GAME_ 200
ENG(FORMAT_SORRY_YOU_CANNOT_JOIN_A_LADDER_WHILE_PLAYING_A_GAME_,"Sorry, you cannot join a ladder while playing a game.")
CHI(FORMAT_SORRY_YOU_CANNOT_JOIN_A_LADDER_WHILE_PLAYING_A_GAME_,"��p, �z����b�U�Ѯɥ[�J�ƦW��.")

#define FORMAT_SORRY_YOU_CANNOT_DROP_A_LADDER_WHILE_PLAYING_A_GAME_ 201
ENG(FORMAT_SORRY_YOU_CANNOT_DROP_A_LADDER_WHILE_PLAYING_A_GAME_,"Sorry, you cannot drop a ladder while playing a game.")
CHI(FORMAT_SORRY_YOU_CANNOT_DROP_A_LADDER_WHILE_PLAYING_A_GAME_,"��p, �z�b�U�Ѯɤ��i���}�ƦW��.")

#define FORMAT_SORRY_YOU_MUST_REGISTER_TO_PLAY_ON_THE_LADDER_ 202
ENG(FORMAT_SORRY_YOU_MUST_REGISTER_TO_PLAY_ON_THE_LADDER_,"Sorry, you must register to play on the ladder.")
CHI(FORMAT_SORRY_YOU_MUST_REGISTER_TO_PLAY_ON_THE_LADDER_,"��p, �z�����ϥΤw���U���b���~��[�J�ƦW��.")

#define FORMAT_THERE_ARE_NO_PLAYERS_THAT_MATCH_YOUR_FLAG_SET_n 203
ENG(FORMAT_THERE_ARE_NO_PLAYERS_THAT_MATCH_YOUR_FLAG_SET_n,"There are no players that match your flag set.\n")
CHI(FORMAT_THERE_ARE_NO_PLAYERS_THAT_MATCH_YOUR_FLAG_SET_n,"�S��������@��ŦX�z�M����󪺨ϥΪ�.\n")

#define FORMAT_d_PLAYERS_FOUND_n 204
ENG(FORMAT_d_PLAYERS_FOUND_n,"%d players found.\n")
CHI(FORMAT_d_PLAYERS_FOUND_n,"��� %d ��ϥΪ�.\n")

#define FORMAT_POSITION_NAME_W_L_DATE_LAST_PLAYEDn 205
ENG(FORMAT_POSITION_NAME_W_L_DATE_LAST_PLAYEDn,"Position      Name       W    L       Date last played\n")
CHI(FORMAT_POSITION_NAME_W_L_DATE_LAST_PLAYEDn," �� �W        �W�r      ��   ��       �̫�什���\n")

#define FORMAT_THERE_ARE_NO_PLAYERS_LOGGED_IN_THAT_MATCH_YOUR_FLAG_SET_ 206
ENG(FORMAT_THERE_ARE_NO_PLAYERS_LOGGED_IN_THAT_MATCH_YOUR_FLAG_SET_,"There are no players logged in that match your flag set.")
CHI(FORMAT_THERE_ARE_NO_PLAYERS_LOGGED_IN_THAT_MATCH_YOUR_FLAG_SET_,"�u�W�S���ŦX�ҭn�D���󪺨ϥΪ�.")

#define FORMAT_INFO_NAME_RANK_19_9_IDLE_RANK_INFOn 207
ENG(FORMAT_INFO_NAME_RANK_19_9_IDLE_RANK_INFOn," Info     Name       Rank  19  9  Idle Rank Info\n")
CHI(FORMAT_INFO_NAME_RANK_19_9_IDLE_RANK_INFOn," ��T     �W�r       �ѤO  19  9  �o�b �ѤO�y�z \n")

#define FORMAT_INFO_NAME_IDLE_RANK_INFO_NAME_IDLE_RANKn 208
ENG(FORMAT_INFO_NAME_IDLE_RANK_INFO_NAME_IDLE_RANKn," Info       Name       Idle   Rank |  Info       Name       Idle   Rank\n")
CHI(FORMAT_INFO_NAME_IDLE_RANK_INFO_NAME_IDLE_RANKn," ��T       �W�r       �o�b   �ѤO |  ��T       �W�r       �o�b   �ѤO\n")

#define FORMAT_d_d_I_ 209
ENG(FORMAT_d_d_I_,"                ******** %3d &l4Q$M  A`&@ %d '=4Q6i&f$$ ********")
CHI(FORMAT_d_d_I_,"                ******** %3d Players %d Total Games ********")

#define FORMAT_d_n 210
ENG(FORMAT_d_n,"%d players found.\n")
CHI(FORMAT_d_n,"��� %d ��Ѥ�.\n")

#define FORMAT_PLAYER_snsGAME_GO_1_n 211
ENG(FORMAT_PLAYER_snsGAME_GO_1_n,"Player:      %s\n%sGame:        go (1)\n")
CHI(FORMAT_PLAYER_snsGAME_GO_1_n,"�ϥΪ�:            %s\n")

#define FORMAT_RATING_ss_dnsRATED_GAMES_dnsRANK_s_dn 212
ENG(FORMAT_RATING_ss_dnsRATED_GAMES_dnsRANK_s_dn,"Rating:     %3.3s%s   %d\n%sRated Games:      %d\n%sRank:  %s   %d\n")
CHI(FORMAT_RATING_ss_dnsRATED_GAMES_dnsRANK_s_dn,"�{�w/�ۭq �ѤO:    %s%s (%d) / %s\n%s���ħ���:          %d\n")

#define FORMAT_WINS_dnsLOSSES_dn 213
ENG(FORMAT_WINS_dnsLOSSES_dn,"Wins:         %d\n%sLosses:       %d\n")
CHI(FORMAT_WINS_dnsLOSSES_dn,"��/��:             %d / %d\n")

#define FORMAT_LAST_ACCESS_GMT_NOT_ON_sn 214
ENG(FORMAT_LAST_ACCESS_GMT_NOT_ON_sn,"Last Access(GMT):   (Not on)    %s\n")
CHI(FORMAT_LAST_ACCESS_GMT_NOT_ON_sn,"�W�^�s�u(GMT):     (���W�u)  %s\n")

#define FORMAT_NEVER_CONNECTED_ 215
ENG(FORMAT_NEVER_CONNECTED_,"Never connected.")
CHI(FORMAT_NEVER_CONNECTED_,"�q���s�u")

#define FORMAT_IDLE_TIME_ON_SERVER_sn 216
ENG(FORMAT_IDLE_TIME_ON_SERVER_sn,"Idle Time:  (On server) %s\n")
CHI(FORMAT_IDLE_TIME_ON_SERVER_sn,"�o�b�ɶ�:          (�b�u�W) %s\n")

#define FORMAT_PLAYING_IN_GAME_d_I_n 217
ENG(FORMAT_PLAYING_IN_GAME_d_I_n,"Playing in game:  %d (I)\n")
CHI(FORMAT_PLAYING_IN_GAME_d_I_n,"�i�����:          %d (I)\n")

#define FORMAT_OBSERVING_GAME_dn 218
ENG(FORMAT_OBSERVING_GAME_dn,"Observing game:  %d\n")
CHI(FORMAT_OBSERVING_GAME_dn,"�[�ݴѧ�:          ")

#define FORMAT_ADDRESS_s_s_n 219
ENG(FORMAT_ADDRESS_s_s_n,"Address: %s [%s]\n")
CHI(FORMAT_ADDRESS_s_s_n,"�q�l�l��H�c:      %s [%s]\n")

#define FORMAT_ADDRESS_sn 220
ENG(FORMAT_ADDRESS_sn,"Address: %s\n")
CHI(FORMAT_ADDRESS_sn,"�q�l�l��H�c:      %s\n")

#define FORMAT_REG_DATE_sn 221
ENG(FORMAT_REG_DATE_sn,"Reg date: %s\n")
CHI(FORMAT_REG_DATE_sn,"���U���:          %s\n")

#define FORMAT_PLAYING_GAME_d_s_VS_s_n 222
ENG(FORMAT_PLAYING_GAME_d_s_VS_s_n,"(playing game %d: %s vs. %s)\n")
CHI(FORMAT_PLAYING_GAME_d_s_VS_s_n,"                   (���b�i�� %d: %s vs. %s)\n")

#define FORMAT_BUSY_s_n 223
ENG(FORMAT_BUSY_s_n,"Busy: [%s]\n")
CHI(FORMAT_BUSY_s_n,"���L��:            [%s]\n")

#define FORMAT_UNREG_s_IS_NOT_A_REGISTERED_PLAYER_ 224
ENG(FORMAT_UNREG_s_IS_NOT_A_REGISTERED_PLAYER_,"Unreg: %s is NOT a registered player.")
CHI(FORMAT_UNREG_s_IS_NOT_A_REGISTERED_PLAYER_,"�����U:            %s �٨S�����U��.\n")

#define FORMAT_RANK_INFO_sn 225
ENG(FORMAT_RANK_INFO_sn,"Rank Info: %s\n")
CHI(FORMAT_RANK_INFO_sn,"����ڪ��ѤO:      %s\n")

#define FORMAT_GAMES_AS_B_d_GAMES_AS_W_dn 226
ENG(FORMAT_GAMES_AS_B_d_GAMES_AS_W_dn,"Games as B:  %-4d  Games as W : %-4d\n")
CHI(FORMAT_GAMES_AS_B_d_GAMES_AS_W_dn,"�� ��/�� ����:     %d / %d\n")

#define FORMAT_ADMIN_LEVEL_dn 227
ENG(FORMAT_ADMIN_LEVEL_dn,"Admin Level: %d\n")
CHI(FORMAT_ADMIN_LEVEL_dn,"�޲z������:        %d\n")

#define FORMAT_LANGUAGE_dn 228
ENG(FORMAT_LANGUAGE_dn,"language           %d\n")
CHI(FORMAT_LANGUAGE_dn,"�y��:              %d\n")

#define FORMAT_INFO_s 229
ENG(FORMAT_INFO_s,"Info: %s")
CHI(FORMAT_INFO_s,"��T: %s")

#define FORMAT_SEE_ALSO_qVARIABLES_sq 230
ENG(FORMAT_SEE_ALSO_qVARIABLES_sq,"See also: \"variables %s\"")
CHI(FORMAT_SEE_ALSO_qVARIABLES_sq,"�����T��:          \"variables %s\"")

#define FORMAT_THE_CURRENT_TIME_GMT_IS_sn 231
ENG(FORMAT_THE_CURRENT_TIME_GMT_IS_sn,"The current time (GMT) is:  %s\n")
CHI(FORMAT_THE_CURRENT_TIME_GMT_IS_sn,"�{�b�ɶ� (GMT):  %s\n")

#define FORMAT_THE_CURRENT_LOCAL_TIME_IS_sn 232
ENG(FORMAT_THE_CURRENT_LOCAL_TIME_IS_sn,"The current local time is:  %s\n")
CHI(FORMAT_THE_CURRENT_LOCAL_TIME_IS_sn,"�{�b���a�ɶ���:  %s\n")

#define FORMAT_THE_WORLD_HAS_EXISTED_SINCE_s_EST_n 233
ENG(FORMAT_THE_WORLD_HAS_EXISTED_SINCE_s_EST_n,"The world has existed since %s EST.\n")
CHI(FORMAT_THE_WORLD_HAS_EXISTED_SINCE_s_EST_n,"LGS �q %s EST �ɶ}�l�ҰʡC\n")

#define FORMAT_UP_FOR_sn 234
ENG(FORMAT_UP_FOR_sn,"Up for: %s\n")
CHI(FORMAT_UP_FOR_sn,"�}�F�o��[: %s\n")

#define FORMAT_PLAYER_LIMIT_dn 235
ENG(FORMAT_PLAYER_LIMIT_dn,"Player limit: %d\n")
CHI(FORMAT_PLAYER_LIMIT_dn,"�H�ƤW��: %d\n")

#define FORMAT_MOVE_LIMIT_2_147_483_648n 236
ENG(FORMAT_MOVE_LIMIT_2_147_483_648n,"Move limit: 2,147,483,648\n")

#define FORMAT_GAMES_PLAYED_SINCE_RESTART_dn 237
ENG(FORMAT_GAMES_PLAYED_SINCE_RESTART_dn,"Games played since restart: %d\n")
CHI(FORMAT_GAMES_PLAYED_SINCE_RESTART_dn,"�Ұʫ�i��L������: %d\n")

#define FORMAT_LOGINS_d_LOGOUTS_d_NEW_PLAYERS_dn 238
ENG(FORMAT_LOGINS_d_LOGOUTS_d_NEW_PLAYERS_dn,"Logins: %d   Logouts: %d   New Players: %d\n")
CHI(FORMAT_LOGINS_d_LOGOUTS_d_NEW_PLAYERS_dn,"�i���H��: %d   �����H��: %d   �s���U�ϥΪ�: %d\n")

#define FORMAT_d_I_W_H_F_d_Cn 239
CHI(FORMAT_d_I_W_H_F_d_Cn,"�u�W�� %d ��ϥΪ�, �W���Ұʦܤ��̦h���F %d ��W�u�C\n")

#define FORMAT_Z_B_S_sn 240
ENG(FORMAT_Z_B_S_sn,"�z�{�b�s����: %s\n")
CHI(FORMAT_Z_B_S_sn,"�Ь� %s\n")

#define FORMAT_8_BIT_TESTING_s 241
ENG(FORMAT_8_BIT_TESTING_s,"8 bit testing: %s")
CHI(FORMAT_8_BIT_TESTING_s,"8 bit �r��: %s")

#define FORMAT_8ON 242
ENG(FORMAT_8ON,"8on")
CHI(FORMAT_8ON,"�}")

#define FORMAT_8OFF 243
ENG(FORMAT_8OFF,"8off")
CHI(FORMAT_8OFF,"��")

#define FORMAT_LOCAL_TIME_sn 244
ENG(FORMAT_LOCAL_TIME_sn,"Local time     - %s\n")
CHI(FORMAT_LOCAL_TIME_sn,"��a�ɶ�       - %s\n")

#define FORMAT_GREENWICH_TIME_sn 245
ENG(FORMAT_GREENWICH_TIME_sn,"Greenwich time - %s\n")
CHI(FORMAT_GREENWICH_TIME_sn,"��L�ªv�ɶ�   - %s\n")

#define FORMAT_SETTING_YOUR_s_TO_s 246
ENG(FORMAT_SETTING_YOUR_s_TO_s,"Setting your %s to %s")
CHI(FORMAT_SETTING_YOUR_s_TO_s,"�� %s ����, �ó]�w�L�� %s")
GER(FORMAT_SETTING_YOUR_s_TO_s,"Ihr %s ist zu %s gesetzt.")

#define FORMAT_THERE_WAS_AN_INTERNAL_ERROR_PLEASE_NOTIFY_AN_ADMIN_ 248
ENG(FORMAT_THERE_WAS_AN_INTERNAL_ERROR_PLEASE_NOTIFY_AN_ADMIN_,"There was an internal error.  Please notify an admin!")
CHI(FORMAT_THERE_WAS_AN_INTERNAL_ERROR_PLEASE_NOTIFY_AN_ADMIN_,"�������~, �Чi���t�κ޲z��!")

#define FORMAT_GAME_SET_TO_GO_ 249
ENG(FORMAT_GAME_SET_TO_GO_,"Game set to go.")
CHI(FORMAT_GAME_SET_TO_GO_,"�C���κA�]�����.")

#define FORMAT_SHOUT_SUCCESSFULLY_ 250
ENG(FORMAT_SHOUT_SUCCESSFULLY_,"Shout successfully.")
CHI(FORMAT_SHOUT_SUCCESSFULLY_,"�ۥs���\\")

#define FORMAT_I_H_U_Z_T_Z_ns 251
ENG(FORMAT_I_H_U_Z_T_Z_ns,"Admins available to help you:\n%s")
CHI(FORMAT_I_H_U_Z_T_Z_ns,"�i�H���U�z���t�κ޲z��:\n%s")

#define FORMAT_F_d_T_Z_I_H_U_Z_ 252
ENG(FORMAT_F_d_T_Z_I_H_U_Z_,"Found %d admin%s to help you.")
CHI(FORMAT_F_d_T_Z_I_H_U_Z_,"���F %d �Өt�κ޲z�̥i�H���U�z.")

#define FORMAT_USE_qITq_SUCCESSFULLY_ 253
ENG(FORMAT_USE_qITq_SUCCESSFULLY_,"Use \"it\" successfully.")
CHI(FORMAT_USE_qITq_SUCCESSFULLY_,"�ϥ� it, OK ")

#define FORMAT_NO_USER_NAMED_qsq_IS_LOGGED_IN_ 254
ENG(FORMAT_NO_USER_NAMED_qsq_IS_LOGGED_IN_,"No user named \"%s\" is logged in.")
CHI(FORMAT_NO_USER_NAMED_qsq_IS_LOGGED_IN_,"�S���W�� \"%s\" ���ϥΪ̤W�u.")

#define FORMAT_NO_USER_NAMED_qsq_IS_LOGGED_IN_n 255
ENG(FORMAT_NO_USER_NAMED_qsq_IS_LOGGED_IN_n,"No user named \"%s\" is logged in.\n")
CHI(FORMAT_NO_USER_NAMED_qsq_IS_LOGGED_IN_n,"�S���W�� \"%s\" ���H�b���W.\n")

#define FORMAT_NO_SUCH_EMOTE 256
ENG(FORMAT_NO_SUCH_EMOTE,"No such emote")
CHI(FORMAT_NO_SUCH_EMOTE,"�S���o�� emote")

#define FORMAT_NO_ONE_TO_TELL_ANYTHING_TO_ 257
ENG(FORMAT_NO_ONE_TO_TELL_ANYTHING_TO_,"No one to tell anything to.")
CHI(FORMAT_NO_ONE_TO_TELL_ANYTHING_TO_,"�г]�w�͸ܹ�H.")

#define FORMAT_PLAYER_qsq_IS_CENSORING_YOU_ 258
ENG(FORMAT_PLAYER_qsq_IS_CENSORING_YOU_,"Player \"%s\" is censoring you.")
CHI(FORMAT_PLAYER_qsq_IS_CENSORING_YOU_,"\"%s\" �N�z�C���ڵ��ө���.")

#define FORMAT_PLAYER_s_IS_CENSORING_YOU_ 259
ENG(FORMAT_PLAYER_s_IS_CENSORING_YOU_,"Player %s is censoring you.")
CHI(FORMAT_PLAYER_s_IS_CENSORING_YOU_,"%s �N�z�C���ڵ��ө���.")

#define FORMAT_PLAYER_qsq_ISN_T_LISTENING_TO_UNREGISTERED_TELLS_ 260
ENG(FORMAT_PLAYER_qsq_ISN_T_LISTENING_TO_UNREGISTERED_TELLS_,"Player \"%s\" isn't listening to unregistered tells.")
CHI(FORMAT_PLAYER_qsq_ISN_T_LISTENING_TO_UNREGISTERED_TELLS_,"\"%s\" �����������U�ϥΪ̤����.")

#define FORMAT_WHO_HAS_BEEN_IDLE_d_HOURS_d_MINUTES 261
ENG(FORMAT_WHO_HAS_BEEN_IDLE_d_HOURS_d_MINUTES," who has been idle %d hours %d minutes")
CHI(FORMAT_WHO_HAS_BEEN_IDLE_d_HOURS_d_MINUTES," �L�w�g�o�b�F %d �p�� %d ����")

#define FORMAT_s_IS_BEEPING_YOU_n 262
ENG(FORMAT_s_IS_BEEPING_YOU_n,"%s is beeping you.\n")
CHI(FORMAT_s_IS_BEEPING_YOU_n,"%s �b�I�s�z.\n")

#define FORMAT_BEEPED_s_S_CONSOLE_WHO_s_IDLE_s_ 263
ENG(FORMAT_BEEPED_s_S_CONSOLE_WHO_s_IDLE_s_,"beeped %s's console who %s (idle: %s)")
CHI(FORMAT_BEEPED_s_S_CONSOLE_WHO_s_IDLE_s_,"�V %s �o�X���n. �L�{�b %s (�o�b: %s)")

#define FORMAT_s_HAS_BEEN_IDLE_s 264
ENG(FORMAT_s_HAS_BEEN_IDLE_s,"%s has been idle %s")
CHI(FORMAT_s_HAS_BEEN_IDLE_s,"%s �w�g�o�b�F %s")

#define FORMAT_BEEPED_s_S_CONSOLEs_ 265
ENG(FORMAT_BEEPED_s_S_CONSOLEs_,"beeped %s`s console%s.")
CHI(FORMAT_BEEPED_s_S_CONSOLEs_,"�V %s �o�X���n%s.")

#define FORMAT_YOU_ARE_NOT_PLAYING_OR_OBSERVING_A_GAME_ 266
ENG(FORMAT_YOU_ARE_NOT_PLAYING_OR_OBSERVING_A_GAME_,"You are not playing or observing a game.")
CHI(FORMAT_YOU_ARE_NOT_PLAYING_OR_OBSERVING_A_GAME_,"�z�å����b�i��, �Ϊ��[�ݤ@�ѧ�.")

#define FORMAT_NO_ONE_TO_SAY_ANYTHING_TO_TRY_TELL_ 267
ENG(FORMAT_NO_ONE_TO_SAY_ANYTHING_TO_TRY_TELL_,"No one to say anything to, try tell.")
CHI(FORMAT_NO_ONE_TO_SAY_ANYTHING_TO_TRY_TELL_,"�S�����w�ͤѪ���H, �ո� tell.")

#define FORMAT_SETTING_A_PASSWORD_IS_ONLY_FOR_REGISTERED_PLAYERS_ 268
ENG(FORMAT_SETTING_A_PASSWORD_IS_ONLY_FOR_REGISTERED_PLAYERS_,"Setting a password is only for registered players.")
CHI(FORMAT_SETTING_A_PASSWORD_IS_ONLY_FOR_REGISTERED_PLAYERS_,"�w���U�ϥΪ̤~�����n���K�X.")

#define FORMAT_INCORRECT_PASSWORD_PASSWORD_NOT_CHANGED_ 269
ENG(FORMAT_INCORRECT_PASSWORD_PASSWORD_NOT_CHANGED_,"Incorrect password, password not changed!")
CHI(FORMAT_INCORRECT_PASSWORD_PASSWORD_NOT_CHANGED_,"�±K�X���~, �K�X������!")

#define FORMAT_PASSWORD_CHANGED_TO_qsq_ 270
ENG(FORMAT_PASSWORD_CHANGED_TO_qsq_,"Password changed to \"%s\".")
CHI(FORMAT_PASSWORD_CHANGED_TO_qsq_,"�K�X��אּ \"%s\".")

#define FORMAT_SORRY_NO_LOGIN_INFORMATION_AVAILABLE_ 271
ENG(FORMAT_SORRY_NO_LOGIN_INFORMATION_AVAILABLE_,"Sorry, no login information available.")
CHI(FORMAT_SORRY_NO_LOGIN_INFORMATION_AVAILABLE_,"��p, �S���W������T�i�ѰѦ�.")

#define FORMAT_USE_qTEACHq_FOR_TEACHING_GAMES_ 272
ENG(FORMAT_USE_qTEACHq_FOR_TEACHING_GAMES_,"Use \"Teach\" for Teaching games.")
CHI(FORMAT_USE_qTEACHq_FOR_TEACHING_GAMES_,"�ϥ� \"Teach\" �i��ѽL�о�.")

#define FORMAT_s_IS_NOT_A_RATED_PLAYER_ 273
ENG(FORMAT_s_IS_NOT_A_RATED_PLAYER_,"%s is not a rated player.")
CHI(FORMAT_s_IS_NOT_A_RATED_PLAYER_,"%s �|���o��t�λ{�w�ѤO�C")

#define FORMAT_YOU_ARE_CENSORING_qsq_ 274
ENG(FORMAT_YOU_ARE_CENSORING_qsq_,"You are censoring \"%s\".")
CHI(FORMAT_YOU_ARE_CENSORING_qsq_,"�z�� \"%s\" �C���ڵ��ө���.")

#define FORMAT_INTERNAL_ERROR_REPORT_CIRCUMSTANCES_AND_BUG_PENDFROMTOn 275
ENG(FORMAT_INTERNAL_ERROR_REPORT_CIRCUMSTANCES_AND_BUG_PENDFROMTOn,"Internal error Report circumstances and bug PENDFROMTO\n")
CHI(FORMAT_INTERNAL_ERROR_REPORT_CIRCUMSTANCES_AND_BUG_PENDFROMTOn,"�������~, �Чi���t�κ޲z��!\n")

#define FORMAT_YOU_ARE_NEITHER_PLAYING_NOR_OBSERVING_A_GAME_ 276
ENG(FORMAT_YOU_ARE_NEITHER_PLAYING_NOR_OBSERVING_A_GAME_,"You are neither playing nor observing a game.")
CHI(FORMAT_YOU_ARE_NEITHER_PLAYING_NOR_OBSERVING_A_GAME_,"�z�å����b�i��, ���[�ݬY����.")

#define FORMAT_YOU_ARE_NEITHER_PLAYING_NOR_OBSERVING_A_GAME_n 277
ENG(FORMAT_YOU_ARE_NEITHER_PLAYING_NOR_OBSERVING_A_GAME_n,"You are neither playing nor observing a game.\n")
CHI(FORMAT_YOU_ARE_NEITHER_PLAYING_NOR_OBSERVING_A_GAME_n,"�z�ä����b�i����[�ݤ@����.\n")

#define FORMAT_GAME_NUMBERS_MUST_BE_1_ 278
ENG(FORMAT_GAME_NUMBERS_MUST_BE_1_,"Game numbers must be >= 1.")
CHI(FORMAT_GAME_NUMBERS_MUST_BE_1_,"�ѧ��s������ >= 1")

#define FORMAT_GAME_NUMBERS_MUST_BE_1_n 279
ENG(FORMAT_GAME_NUMBERS_MUST_BE_1_n,"Game numbers must be >= 1.\n")
CHI(FORMAT_GAME_NUMBERS_MUST_BE_1_n,"�ѧ��s������ >= 1\n")

#define FORMAT_NO_SUCH_GAME_n 280
ENG(FORMAT_NO_SUCH_GAME_n,"No such game.\n")
CHI(FORMAT_NO_SUCH_GAME_n,"�S������.\n")

#define FORMAT_INVALID_RANK_VALID_RANKS_ARE_30K_1K_1D_6D_ 281
ENG(FORMAT_INVALID_RANK_VALID_RANKS_ARE_30K_1K_1D_6D_,"Invalid rank.  Valid ranks are: 30K - 1K, 1D - 6D.")

#define FORMAT_RANK_SET_TO_sn 282
ENG(FORMAT_RANK_SET_TO_sn,"Rank set to %s\n")
CHI(FORMAT_RANK_SET_TO_sn,"Messages:\n")

#define FORMAT_PLEASE_INFORM_AN_ADMIN_OF_THIS_THANK_YOU_n 283
ENG(FORMAT_PLEASE_INFORM_AN_ADMIN_OF_THIS_THANK_YOU_n,"Please inform an admin of this. Thank you.\n")
CHI(FORMAT_PLEASE_INFORM_AN_ADMIN_OF_THIS_THANK_YOU_n,"�Чi���t�κ޲z��, ����.\n")
GER(FORMAT_PLEASE_INFORM_AN_ADMIN_OF_THIS_THANK_YOU_n,"Bitte informieren sie einen Administrator.\n")

#define FORMAT_SORRY_YOUR_NOTIFY_LIST_IS_ALREADY_FULL_n 284
ENG(FORMAT_SORRY_YOUR_NOTIFY_LIST_IS_ALREADY_FULL_n,"Sorry, your notify list is already full.\n")

#define FORMAT_YOUR_NOTIFY_LIST_ALREADY_INCLUDES_s_n 285
ENG(FORMAT_YOUR_NOTIFY_LIST_ALREADY_INCLUDES_s_n,"Your notify list already includes %s.\n")

#define FORMAT_YOU_CAN_T_NOTIFY_YOURSELF_n 286
ENG(FORMAT_YOU_CAN_T_NOTIFY_YOURSELF_n,"You can't notify yourself.\n")

#define FORMAT_s_IS_NOW_ON_YOUR_NOTIFY_LIST_n 287
ENG(FORMAT_s_IS_NOW_ON_YOUR_NOTIFY_LIST_n,"%s is now on your notify list.\n")

#define FORMAT_s_IS_REMOVED_FROM_YOUR_NOTIFY_LIST_n 288
ENG(FORMAT_s_IS_REMOVED_FROM_YOUR_NOTIFY_LIST_n,"%s is removed from your notify list.\n")

#define FORMAT_NO_ONE_WAS_REMOVED_FROM_YOUR_NOTIFY_LIST_n 289
ENG(FORMAT_NO_ONE_WAS_REMOVED_FROM_YOUR_NOTIFY_LIST_n,"No one was removed from your notify list.\n")

#define FORMAT_PRESENT_COMPANY_ON_YOUR_NOTIFY_LIST_n 290
ENG(FORMAT_PRESENT_COMPANY_ON_YOUR_NOTIFY_LIST_n,"Present company on your notify list:\n")

#define FORMAT_NO_ONE_FROM_YOUR_NOTIFY_LIST_IS_LOGGED_ON_n 291
ENG(FORMAT_NO_ONE_FROM_YOUR_NOTIFY_LIST_IS_LOGGED_ON_n,"No one from your notify list is logged on.\n")

#define FORMAT_THE_FOLLOWING_PLAYERS_HAVE_YOU_ON_THEIR_NOTIFY_LIST_n 292
ENG(FORMAT_THE_FOLLOWING_PLAYERS_HAVE_YOU_ON_THEIR_NOTIFY_LIST_n,"The following players have you on their notify list:\n")

#define FORMAT_NO_ONE_LOGGED_IN_HAS_YOU_ON_THEIR_NOTIFY_LIST_n 293
ENG(FORMAT_NO_ONE_LOGGED_IN_HAS_YOU_ON_THEIR_NOTIFY_LIST_n,"No one logged in has you on their notify list.\n")

#define FORMAT_NOTIFICATION_s_HAS_DEPARTED_AND_ISN_T_ON_YOUR_NOTIFY_LIST_n 294
ENG(FORMAT_NOTIFICATION_s_HAS_DEPARTED_AND_ISN_T_ON_YOUR_NOTIFY_LIST_n,"Notification: %s has departed and isn't on your notify list.\n")

#define FORMAT_PRESENT_COMPANY_INCLUDES_ 295
ENG(FORMAT_PRESENT_COMPANY_INCLUDES_,"Present company includes:")

#define FORMAT_a 296
ENG(FORMAT_a,"\a")

#define FORMAT_NOTIFICATION_s_HAS_ARRIVED_AND_ISN_T_ON_YOUR_NOTIFY_LIST_n 297
ENG(FORMAT_NOTIFICATION_s_HAS_ARRIVED_AND_ISN_T_ON_YOUR_NOTIFY_LIST_n,"Notification: %s has arrived and isn't on your notify list.\n")

#define FORMAT_NOTIFICATION_s_HAS_s_n 298
ENG(FORMAT_NOTIFICATION_s_HAS_s_n,"Notification: %s has %s.\n")

#define FORMAT_YOUR_s_WAS_NOTED_BY_ 299
ENG(FORMAT_YOUR_s_WAS_NOTED_BY_,"Your %s was noted by:")

#define FORMAT_YOU_CAN_CHECK_YOUR_SCORE_WITH_THE_SCORE_COMMAND_TYPE_DONE_WHEN_FINISHED_n 300
ENG(FORMAT_YOU_CAN_CHECK_YOUR_SCORE_WITH_THE_SCORE_COMMAND_TYPE_DONE_WHEN_FINISHED_n,"You can check your score with the score command, type 'done' when finished.\n")

#define FORMAT_EVENT_STRING_SET_TO_s 301
ENG(FORMAT_EVENT_STRING_SET_TO_s,"Event string set to: %s")

#define FORMAT_SORRY_THIS_IS_ALREADY_A_TEACHING_GAME_ 302
ENG(FORMAT_SORRY_THIS_IS_ALREADY_A_TEACHING_GAME_,"Sorry, this is already a teaching game.")
CHI(FORMAT_SORRY_THIS_IS_ALREADY_A_TEACHING_GAME_,"�����w���]�����ɴ�.")

#define FORMAT_THIS_IS_NOW_A_FREE_UNRATED_TEACHING_GAME_n 303
ENG(FORMAT_THIS_IS_NOW_A_FREE_UNRATED_TEACHING_GAME_n,"This is now a FREE, UNRATED teaching game.\n")
CHI(FORMAT_THIS_IS_NOW_A_FREE_UNRATED_TEACHING_GAME_n,"�o���Ѧ��� Free, ���C�J���������ɴ�\n")

#define FORMAT_KOMI_CAN_ONLY_BE_AN_EVEN_NUMBER_IN_ING_S_RULE_ 304
ENG(FORMAT_KOMI_CAN_ONLY_BE_AN_EVEN_NUMBER_IN_ING_S_RULE_,"Komi can only be an even number in Ing's rule.")
CHI(FORMAT_KOMI_CAN_ONLY_BE_AN_EVEN_NUMBER_IN_ING_S_RULE_,"����ѳW���K�I�����O����.")

#define FORMAT_IN_ING_S_RULE_KOMI_MUST_NOT_BE_MORE_THAN_8_ 305
ENG(FORMAT_IN_ING_S_RULE_KOMI_MUST_NOT_BE_MORE_THAN_8_,"In Ing's rule, komi must not be more than 8.")
CHI(FORMAT_IN_ING_S_RULE_KOMI_MUST_NOT_BE_MORE_THAN_8_,"����ѳW���K�I���i�W�L�K�I.")

#define FORMAT_INVALID_RULE_NUMBER_SEE_qHELP_RULEq_n 306
ENG(FORMAT_INVALID_RULE_NUMBER_SEE_qHELP_RULEq_n,"Invalid rule number. See \"help rule\".\n")
CHI(FORMAT_INVALID_RULE_NUMBER_SEE_qHELP_RULEq_n,"�����T���W�h�s��. �Ь� \"help rule\"\n")

#define FORMAT_ING_S_RULE_ONLY_WORKS_WITH_A_GOBAN_WHOSE_SIZE_IS_ODD_AND_GREATER_THAN_12_ 307
ENG(FORMAT_ING_S_RULE_ONLY_WORKS_WITH_A_GOBAN_WHOSE_SIZE_IS_ODD_AND_GREATER_THAN_12_,"Ing's rule only works with a goban whose size is odd and greater than 12.")

#define FORMAT_SET_TO_JAPANESE_SCORING_RULE_ 308
ENG(FORMAT_SET_TO_JAPANESE_SCORING_RULE_,"Set to Japanese scoring rule.")

#define FORMAT_SET_TO_ING_S_SCORING_RULE_ 309
ENG(FORMAT_SET_TO_ING_S_SCORING_RULE_,"Set to Ing's scoring rule.")

#define FORMAT_SET_TO_JAPANESE_SCORING_RULE_KOMI_IS_SET_TO_f 310
ENG(FORMAT_SET_TO_JAPANESE_SCORING_RULE_KOMI_IS_SET_TO_f,"Set to Japanese scoring rule. Komi is set to %.1f")

#define FORMAT_RULE_IS_NOW_SET_TO_JAPANESE_KOMI_IS_SET_TO_fn 311
ENG(FORMAT_RULE_IS_NOW_SET_TO_JAPANESE_KOMI_IS_SET_TO_fn,"Rule is now set to Japanese. Komi is set to %.1f\n")

#define FORMAT_SET_TO_ING_S_SCORING_RULE_KOMI_IS_SET_TO_d 312
ENG(FORMAT_SET_TO_ING_S_SCORING_RULE_KOMI_IS_SET_TO_d,"Set to Ing's scoring rule. Komi is set to %d")

#define FORMAT_RULE_IS_NOW_SET_TO_THE_ING_S_KOMI_IS_SET_TO_dn 313
ENG(FORMAT_RULE_IS_NOW_SET_TO_THE_ING_S_KOMI_IS_SET_TO_dn,"Rule is now set to the Ing's. Komi is set to %d\n")

#define FORMAT_SET_THE_KOMI_TO_f 314
ENG(FORMAT_SET_THE_KOMI_TO_f,"Set the komi to %.1f")

#define FORMAT_FULL_NAME_s 315
ENG(FORMAT_FULL_NAME_s,"Full Name:   %s")
CHI(FORMAT_FULL_NAME_s,"���W:              %s")

#define FORMAT_nsMUZZLED_s_GMUZZLED_s_BMUZZLED_s_TMUZZLED_s_KMUZZLED_s 316
ENG(FORMAT_nsMUZZLED_s_GMUZZLED_s_BMUZZLED_s_TMUZZLED_s_KMUZZLED_s,"\n%sMuzzled: %s  GMuzzled: %s  BMuzzled: %s  TMuzzled: %s  KMuzzled: %s")

#define FORMAT_GSHOUT_s_KIBITZ_sn 317
ENG(FORMAT_GSHOUT_s_KIBITZ_sn,"gshout                   = %-3.3s       kibitz                       = %-3.3s\n")
CHI(FORMAT_GSHOUT_s_KIBITZ_sn,"��ѧo�� (gshout)        = %-3.3s       ���[�̰Q�� (kibitz)          = %-3.3s\n")

#define FORMAT_SHOUT_s_OPEN_sn 318
ENG(FORMAT_SHOUT_s_OPEN_sn,"shout                    = %-3.3s       open                         = %-3.3s\n")
CHI(FORMAT_SHOUT_s_OPEN_sn,"�o�� (shout)             = %-3.3s       �����D�� (open)              = %-3.3s\n")

#define FORMAT_BELL_s_TELL_sn 319
ENG(FORMAT_BELL_s_TELL_sn,"bell                     = %-3.3s       tell                         = %-3.3s\n")
CHI(FORMAT_BELL_s_TELL_sn,"�a�n (bell)              = %-3.3s       �w���� (tell)              = %-3.3s\n")

#define FORMAT_ROBOT_s_PIN_sn 320
ENG(FORMAT_ROBOT_s_PIN_sn,"robot                    = %-3.3s       pin                          = %-3.3s\n")
CHI(FORMAT_ROBOT_s_PIN_sn,"�q���{�� (robot)         = %-3.3s       �ӤH��T (pin)               = %-3.3s\n")

#define FORMAT_LOOKING_s_VERBOSE_sn 321
ENG(FORMAT_LOOKING_s_VERBOSE_sn,"looking                  = %-3.3s       verbose                      = %-3.3s\n")
CHI(FORMAT_LOOKING_s_VERBOSE_sn,"����什�� (looking)     = %-3.3s       ��r�ѽL (verbose)           = %-3.3s\n")

#define FORMAT_PRIVATE_s_ROPEN_sn 322
ENG(FORMAT_PRIVATE_s_ROPEN_sn,"private                  = %-3.3s       ropen                        = %-3.3s\n")
CHI(FORMAT_PRIVATE_s_ROPEN_sn,"�p�H�什 (private)       = %-3.3s       ���w�什 (ropen)             = %-3.3s\n")

#define FORMAT_AUTOMAIL_s_GIN_sn 323
ENG(FORMAT_AUTOMAIL_s_GIN_sn,"automail                 = %-3.3s       gin                          = %-3.3s\n")
CHI(FORMAT_AUTOMAIL_s_GIN_sn,"�۰ʱH�� (automail)      = %-3.3s       �ѧ����i (gin)               = %-3.3s\n")

#define FORMAT_CLIENT_s_LSHOUT_sn 324
ENG(FORMAT_CLIENT_s_LSHOUT_sn,"client                   = %-3.3s       lshout                       = %-3.3s\n")
CHI(FORMAT_CLIENT_s_LSHOUT_sn,"���ݵ{�� (client)        = %-3.3s       �ƦW�ɤ��i (lshout)          = %-3.3s\n")

#define FORMAT_TIME_d_BYO_STONES_dn 325
ENG(FORMAT_TIME_d_BYO_STONES_dn,"time                     = %2.2d        byo_stones                   = %2d\n")
CHI(FORMAT_TIME_d_BYO_STONES_dn,"�򥻮ɭ� (time)          = %2.2d        Ū������ (byo_stones)    = %2d\n")

#define FORMAT_SIZE_d_NOTIFIED_dn 326
ENG(FORMAT_SIZE_d_NOTIFIED_dn,"size                     = %2.2d        notified                     = %d\n")
CHI(FORMAT_SIZE_d_NOTIFIED_dn,"�ѽL�j�p (size)          = %2.2d        notifiedby (notified)        = %d\n")

#define FORMAT_WIDTH_d_HEIGHT_dn 327
ENG(FORMAT_WIDTH_d_HEIGHT_dn,"width                    = %2.2d        height                       = %2d\n")
CHI(FORMAT_WIDTH_d_HEIGHT_dn,"�e�� (width)             = %2.2d        ���� (height)                = %2d\n")

#define FORMAT_BYO_TIME_d_SOMETHING_CHINESE_dn 328
ENG(FORMAT_BYO_TIME_d_SOMETHING_CHINESE_dn,"byo_time                 = %2.2d        Something_chinese            = %d\n")
CHI(FORMAT_BYO_TIME_d_SOMETHING_CHINESE_dn,"Ū��ɭ� (byo_time)      = %2.2d        ���D�ƥ�                     = %d\n")

#define FORMAT_MORE_CHINESE_d_EXTPROMPT_sn 329
ENG(FORMAT_MORE_CHINESE_d_EXTPROMPT_sn,"More_chinese             = %2.2d        extprompt                    = %-3.3s\n")
CHI(FORMAT_MORE_CHINESE_d_EXTPROMPT_sn,"���y                     = %2.2d        �������ܲŸ� (extprompt)     = %-3.3s\n")

#define FORMAT_YES 330
ENG(FORMAT_YES,"yes")
CHI(FORMAT_YES,"�O")

#define FORMAT_NO 331
ENG(FORMAT_NO,"No")
CHI(FORMAT_NO,"�_")

#define FORMAT_WHITE 332
ENG(FORMAT_WHITE,"White")
CHI(FORMAT_WHITE,"��")

#define FORMAT_BLACK 333
ENG(FORMAT_BLACK,"Black")
CHI(FORMAT_BLACK,"��")

#define FORMAT_GIVE 334
ENG(FORMAT_GIVE,"Give")
CHI(FORMAT_GIVE,"��")

#define FORMAT_TAKE 335
ENG(FORMAT_TAKE,"Take")
CHI(FORMAT_TAKE,"��")

#define FORMAT_ON 336
ENG(FORMAT_ON,"On")
CHI(FORMAT_ON,"�}")

#define FORMAT_OFF 337
ENG(FORMAT_OFF,"Off")
CHI(FORMAT_OFF,"��")

#define FORMAT_WHO_IS_PLAYING_A_GAME_ 338
ENG(FORMAT_WHO_IS_PLAYING_A_GAME_," (who is playing a game)")
CHI(FORMAT_WHO_IS_PLAYING_A_GAME_," (�L���b�U�Ѥ�)")

#define FORMAT_SECRET 339
ENG(FORMAT_SECRET,"SECRET")
CHI(FORMAT_SECRET,"���K")

#define FORMAT_READ_ONLY 340
ENG(FORMAT_READ_ONLY,"READ ONLY")
CHI(FORMAT_READ_ONLY,"��Ū")

#define FORMAT_READ_WRITE 341
ENG(FORMAT_READ_WRITE,"READ/WRITE")
CHI(FORMAT_READ_WRITE,"Ū/�g")

#define FORMAT_STILL 342
ENG(FORMAT_STILL,"still")
CHI(FORMAT_STILL,"���M")

#define FORMAT_NOW 343
ENG(FORMAT_NOW,"now")
CHI(FORMAT_NOW,"�{�b")

#define FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_SHOUT_COMMAND_ 344
ENG(FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_SHOUT_COMMAND_,"Only registered players can use the shout command.")
CHI(FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_SHOUT_COMMAND_,"�u���w���U�ϥΪ̥i�� shout ���O.")

#define FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_BEEP_COMMAND_ 345
ENG(FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_BEEP_COMMAND_,"Only registered players can use the beep command.")
CHI(FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_BEEP_COMMAND_,"�u���w���U�ϥΪ̥i�ϥ� beep ���O.")

#define FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_GSHOUT_COMMAND_ 346
ENG(FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_GSHOUT_COMMAND_,"Only registered players can use the gshout command.")
CHI(FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_GSHOUT_COMMAND_,"�u���w���U�ϥΪ̤~��ϥ� gshout ���O.")

#define FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_IT_COMMAND_ 347
ENG(FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_IT_COMMAND_,"Only registered players can use the it command.")
CHI(FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_IT_COMMAND_,"�u���w���U�ϥΪ̤~�i�ϥ� it ���O.")

#define FORMAT_YOU_MUST_BE_A_RATED_PLAYER_TO_USE_THIS_COMMAND_ 348
ENG(FORMAT_YOU_MUST_BE_A_RATED_PLAYER_TO_USE_THIS_COMMAND_,"You must be a rated player to use this command.")
CHI(FORMAT_YOU_MUST_BE_A_RATED_PLAYER_TO_USE_THIS_COMMAND_,"�z�����O�w��t�λ{�w�ѤO���ϥΪ̤~��ϥΦ����O�C")

#define FORMAT_s_DECLINES_YOUR_REQUEST_FOR_A_MATCH_n 349
ENG(FORMAT_s_DECLINES_YOUR_REQUEST_FOR_A_MATCH_n,"%s declines your request for a match.\n")
CHI(FORMAT_s_DECLINES_YOUR_REQUEST_FOR_A_MATCH_n,"%s �ڵ��F�z���什�ܽ�.\n")

#define FORMAT_s_UPDATES_THE_MATCH_REQUEST_n 350
ENG(FORMAT_s_UPDATES_THE_MATCH_REQUEST_n,"%s updates the match request.\n")
CHI(FORMAT_s_UPDATES_THE_MATCH_REQUEST_n,"%s ���F�什�ܽЪ��]�w.\n")

#define FORMAT_MATCH_dXd_IN_d_MINUTES_REQUESTED_WITH_s_n 351
ENG(FORMAT_MATCH_dXd_IN_d_MINUTES_REQUESTED_WITH_s_n,"Match [%dx%d] in %d minutes requested with %s.\n")

#define FORMAT_USE_RMATCH_s_d_d_OR_DECLINE_s_TO_RESPOND_n 352
ENG(FORMAT_USE_RMATCH_s_d_d_OR_DECLINE_s_TO_RESPOND_n,"Use <rmatch %s %d %d> or <decline %s> to respond.\n")

#define FORMAT_J_d_C 353
ENG(FORMAT_J_d_C,"E*,m.I--%267$j)s %d $@DA!C")
CHI(FORMAT_J_d_C,"Byo Time must be greater than %d minutes.")

#define FORMAT_THERE_WAS_A_PROBLEM_CREATING_THE_NEW_MATCH_ 354
ENG(FORMAT_THERE_WAS_A_PROBLEM_CREATING_THE_NEW_MATCH_,"There was a problem creating the new match.")
CHI(FORMAT_THERE_WAS_A_PROBLEM_CREATING_THE_NEW_MATCH_,"�什�]�w�X�F���D.")

#define FORMAT_SORRY_YOU_CAN_T_HAVE_ANY_MORE_PENDING_MATCHES_ 355
ENG(FORMAT_SORRY_YOU_CAN_T_HAVE_ANY_MORE_PENDING_MATCHES_,"Sorry, you can't have any more pending matches.")
CHI(FORMAT_SORRY_YOU_CAN_T_HAVE_ANY_MORE_PENDING_MATCHES_,"��p, �z����A��O�H���X�什�ܽ�.")

#define FORMAT_SORRY_s_CAN_T_HAVE_ANY_MORE_PENDING_MATCHES_ 356
ENG(FORMAT_SORRY_s_CAN_T_HAVE_ANY_MORE_PENDING_MATCHES_,"Sorry, %s can't have any more pending matches.")
CHI(FORMAT_SORRY_s_CAN_T_HAVE_ANY_MORE_PENDING_MATCHES_,"��p, %s ����A����h�什�ܽ�.")

#define FORMAT_PLAYER_qsq_IS_NOT_OPEN_TO_MATCH_REQUESTS_ 357
ENG(FORMAT_PLAYER_qsq_IS_NOT_OPEN_TO_MATCH_REQUESTS_,"Player \"%s\" is not open to match requests.")
CHI(FORMAT_PLAYER_qsq_IS_NOT_OPEN_TO_MATCH_REQUESTS_,"\"%s\" �ثe�������什.")

#define FORMAT_PLAYER_qsq_IS_INVOLVED_IN_ANOTHER_GAME_ 358
ENG(FORMAT_PLAYER_qsq_IS_INVOLVED_IN_ANOTHER_GAME_,"Player \"%s\" is involved in another game.")
CHI(FORMAT_PLAYER_qsq_IS_INVOLVED_IN_ANOTHER_GAME_,"\"%s\" ���b�U�Ѥ�.")

#define FORMAT_THE_SIZE_OF_GO_BOARD_MUST_BE_AN_ODD_INTEGER_ 359
ENG(FORMAT_THE_SIZE_OF_GO_BOARD_MUST_BE_AN_ODD_INTEGER_,"The size of go board must be an odd integer.")
CHI(FORMAT_THE_SIZE_OF_GO_BOARD_MUST_BE_AN_ODD_INTEGER_,"�ѽL�ؤo�����O�_��.")

#define FORMAT_THERE_WAS_A_PROBLEM_CREATING_THE_NEW_MATCH_n 360
ENG(FORMAT_THERE_WAS_A_PROBLEM_CREATING_THE_NEW_MATCH_n,"There was a problem creating the new match.\n")
CHI(FORMAT_THERE_WAS_A_PROBLEM_CREATING_THE_NEW_MATCH_n,"�什�]�w�X�F���D.")

#define FORMAT_sSORRY_s_CAN_T_HAVE_ANY_MORE_PENDING_MATCHES_ 361
ENG(FORMAT_sSORRY_s_CAN_T_HAVE_ANY_MORE_PENDING_MATCHES_,"%sSorry, %s can't have any more pending matches.")
CHI(FORMAT_sSORRY_s_CAN_T_HAVE_ANY_MORE_PENDING_MATCHES_,"%s��p, %s ����A����h�什�ܽ�.")

#define FORMAT_sDECLINING_OFFER_FROM_s_AND_OFFERING_NEW_MATCH_PARAMETERS_n 362
ENG(FORMAT_sDECLINING_OFFER_FROM_s_AND_OFFERING_NEW_MATCH_PARAMETERS_n,"%sDeclining offer from %s and offering new match parameters.\n")
CHI(FORMAT_sDECLINING_OFFER_FROM_s_AND_OFFERING_NEW_MATCH_PARAMETERS_n,"%s���P�N %s ���ܽШô��X�s���什�]�w�Ѽ�.\n")

#define FORMAT_UPDATING_MATCH_REQUEST 363
ENG(FORMAT_UPDATING_MATCH_REQUEST,"Updating match request")
CHI(FORMAT_UPDATING_MATCH_REQUEST,"���什�ܽг]�w")

#define FORMAT_REQUESTING_MATCH_IN_d_MIN_WITH_s_AS_s_n 364
ENG(FORMAT_REQUESTING_MATCH_IN_d_MIN_WITH_s_AS_s_n,"Requesting match in %d min with %s as %s.\n")
CHI(FORMAT_REQUESTING_MATCH_IN_d_MIN_WITH_s_AS_s_n,"�ܽ� %s ��%s�ӤU��, �򥻮ɭ� %d ����.\n")

#define FORMAT_YOU_ARE_NOT_RATED_SO_I_CANNOT_SUGGEST_n 365
ENG(FORMAT_YOU_ARE_NOT_RATED_SO_I_CANNOT_SUGGEST_n,"You are not rated, so I cannot suggest.\n")
CHI(FORMAT_YOU_ARE_NOT_RATED_SO_I_CANNOT_SUGGEST_n,"�z�|����o�{�w�ѤO, �ҥH�ڵL�k���X��ĳ.\n")

#define FORMAT_PLEASE_DISCUSS_HANDICAPS_WITH_YOUR_OPPONENT_n 366
ENG(FORMAT_PLEASE_DISCUSS_HANDICAPS_WITH_YOUR_OPPONENT_n,"Please discuss handicaps with your opponent.\n")
CHI(FORMAT_PLEASE_DISCUSS_HANDICAPS_WITH_YOUR_OPPONENT_n,"�лP�z�����Q�׾A���t.\n")

#define FORMAT_DECLINING_OFFER_FROM_s_AND_OFFERING_NEW_MATCH_PARAMETERS_n 367
ENG(FORMAT_DECLINING_OFFER_FROM_s_AND_OFFERING_NEW_MATCH_PARAMETERS_n,"Declining offer from %s and offering new match parameters.\n")
CHI(FORMAT_DECLINING_OFFER_FROM_s_AND_OFFERING_NEW_MATCH_PARAMETERS_n,"���P�N %s ���ܽШô��X�s���什�]�w�Ѽ�.\n")

#define FORMAT_REQUESTING_RATED_MATCH_IN_d_MIN_WITH_s_n 368
ENG(FORMAT_REQUESTING_RATED_MATCH_IN_d_MIN_WITH_s_n,"Requesting rated match in %d min with %s.\n")
CHI(FORMAT_REQUESTING_RATED_MATCH_IN_d_MIN_WITH_s_n,"�ܽ� %s �U��, �򥻮ɭ� %d ����.\n")

#define FORMAT_MATCH_dXd_IN_d_MINUTES_REQUESTED_WITH_s_AS_s_n 369
ENG(FORMAT_MATCH_dXd_IN_d_MINUTES_REQUESTED_WITH_s_AS_s_n,"Match [%dx%d] in %d minutes requested with %s as %s.\n")

#define FORMAT_nn 370
ENG(FORMAT_nn,"\n\n")

#define FORMAT_d_PLAYERS_d_TOTAL_GAMES_ 371
ENG(FORMAT_d_PLAYERS_d_TOTAL_GAMES_,"                ******** %3d Players %d Total Games ********")

#define FORMAT_AUTOMATIC_SERVER_SHUTDOWN_n 372
ENG(FORMAT_AUTOMATIC_SERVER_SHUTDOWN_n,"    **** Automatic Server shutdown. ****\n")

#define FORMAT_SERVER_SHUTDOWN_CANCELED_BY_s_n 373
ENG(FORMAT_SERVER_SHUTDOWN_CANCELED_BY_s_n,"    **** Server shutdown canceled by %s. ****\n")

#define FORMAT_SERVER_SHUTDOWN_STARTED_BY_s_n 374
ENG(FORMAT_SERVER_SHUTDOWN_STARTED_BY_s_n,"    **** Server shutdown started by %s. ****\n")

#define FORMAT_SERVER_SHUTTING_DOWN_IMMEDIATELY_n 375
ENG(FORMAT_SERVER_SHUTTING_DOWN_IMMEDIATELY_n,"    **** Server shutting down immediately. ****\n")

#define FORMAT_sn 376
ENG(FORMAT_sn,"%s\n")

#define FORMAT_NAME_FROM_LAST_TELL_CHAN_STATUSn 377
ENG(FORMAT_NAME_FROM_LAST_TELL_CHAN_STATUSn,"   Name        From        Last Tell  Chan Status\n")

#define FORMAT_UNKNOWN_COMMANDn 378
ENG(FORMAT_UNKNOWN_COMMANDn,"  UNKNOWN COMMAND\n")

#define FORMAT_d_s_d_d_sn 379
ENG(FORMAT_d_s_d_d_sn," %3d      %-10s    %3d  %3d  %s\n")

#define FORMAT_d_ 380
ENG(FORMAT_d_,"[%d]")

#define FORMAT_s 381
ENG(FORMAT_s,"%s")

#define FORMAT_IT_IS_YOUR_PARTNER_S_MOVE_n 382
ENG(FORMAT_IT_IS_YOUR_PARTNER_S_MOVE_n,"It is your partner's move.\n")

#define FORMAT_LADDERd_GAME_MATCH_d_s_d_VS_s_d_n 383
ENG(FORMAT_LADDERd_GAME_MATCH_d_s_d_VS_s_d_n,"!! Ladder%d Game: Match %d  %s (%d) vs. %s (%d) !!\n")

#define FORMAT_LADDER19_RESULT_s_TAKES_POSITION_d_FROM_s_n 384
ENG(FORMAT_LADDER19_RESULT_s_TAKES_POSITION_d_FROM_s_n,"!! Ladder19 Result: %s takes position %d from %s !!\n")

#define FORMAT_LADDER9_RESULT_s_TAKES_POSITION_d_FROM_s_n 385
ENG(FORMAT_LADDER9_RESULT_s_TAKES_POSITION_d_FROM_s_n,"!! Ladder9 Result: %s takes position %d from %s !!\n")

#define FORMAT_s_sn 386
ENG(FORMAT_s_sn,"%s --> %s\n")

#define FORMAT_s_HAS_JOINED_THE_ADMIN_CHANNEL_n 387
ENG(FORMAT_s_HAS_JOINED_THE_ADMIN_CHANNEL_n,"## --- ##: %s has joined the ADMIN channel.\n")

#define FORMAT_s_JUST_NUKED_s_n 388
ENG(FORMAT_s_JUST_NUKED_s_n,"## --- ##: %s just nuked %s.\n")

#define FORMAT_s_JUST_TURNED_s_SHOUTS_n 389
ENG(FORMAT_s_JUST_TURNED_s_SHOUTS_n,"## --- ##: %s just turned %s shouts.\n")

#define FORMAT_POSE_s_AS_s_s_n 390
ENG(FORMAT_POSE_s_AS_s_s_n,"## --- ##: POSE: %s as %s: > %s <\n")

#define FORMAT_ADMIN_s_s_n 391
ENG(FORMAT_ADMIN_s_s_n,"## ADMIN ##: %s : > %s <\n")

#define FORMAT_s_s_s_d_s_dn 392
ENG(FORMAT_s_s_s_d_s_dn,"%-10s %-15.15s %-10.10s %-4.2d %s %-3.2d\n")

#define FORMAT_d_sn 393
ENG(FORMAT_d_sn,"%2d: %s\n")

#define FORMAT_s_s_sn 394
ENG(FORMAT_s_s_sn,"%3s (%s) %s\n")

#define FORMAT_d_MINUTES_WERE_ADDED_TO_YOUR_OPPONENTS_CLOCK 395
ENG(FORMAT_d_MINUTES_WERE_ADDED_TO_YOUR_OPPONENTS_CLOCK,"%d minutes were added to your opponents clock")

#define FORMAT_d_OFFERS_WITHDRAWN 396
ENG(FORMAT_d_OFFERS_WITHDRAWN,"%d offers withdrawn")

#define FORMAT_d_PLAYERS_IN_9X9_LADDER_PLAYER_s_IS_1_n 397
ENG(FORMAT_d_PLAYERS_IN_9X9_LADDER_PLAYER_s_IS_1_n,"%d players in  9x9  ladder.  Player '%s' is #1!\n")

#define FORMAT_d_PLAYERS_IN_19X19_LADDER_PLAYER_s_IS_1_n 398
ENG(FORMAT_d_PLAYERS_IN_19X19_LADDER_PLAYER_s_IS_1_n,"%d players in 19x19 ladder.  Player '%s' is #1!\n")

#define FORMAT_d_s_sn 399
ENG(FORMAT_d_s_sn,"%d:%s: %s\n")

#define FORMAT_s_n 400
ENG(FORMAT_s_n,"^ %s ^\n")

#define FORMAT_s_H_CAP_d_KOMI_fn 401
ENG(FORMAT_s_H_CAP_d_KOMI_fn,"%s      H-cap %d Komi  %.1f\n")

#define FORMAT_s_0_O_WHITE_n 402
ENG(FORMAT_s_0_O_WHITE_n,"%s    #0 O (White)\n")

#define FORMAT_s_c_d_sn 403
ENG(FORMAT_s_c_d_sn,"%s    %c #%3d %s\n")

#define FORMAT_s_d_s_s_n 404
ENG(FORMAT_s_d_s_s_n,"%s   #%d %s (%s)\n")

#define FORMAT_s_LAST_MOVE_sn 405
ENG(FORMAT_s_LAST_MOVE_sn,"%s   Last Move: %s\n")

#define FORMAT_s_LAST_MOVE_n 406
ENG(FORMAT_s_LAST_MOVE_n,"%s   Last Move:\n")

#define FORMAT_s_BL_TIME_ssn 407
ENG(FORMAT_s_BL_TIME_ssn,"%s  Bl Time %s%s\n")

#define FORMAT_s_CAPTURED_BY_dn 408
ENG(FORMAT_s_CAPTURED_BY_dn,"%s  Captured by #: %d\n")

#define FORMAT_s_CAPTURED_BY_O_dn 409
ENG(FORMAT_s_CAPTURED_BY_O_dn,"%s  Captured by O: %d\n")

#define FORMAT_s_WH_TIME_ssn 410
ENG(FORMAT_s_WH_TIME_ssn,"%s  Wh Time %s%s\n")

#define FORMAT_s_ss_d_d_d_T_f_dn 411
ENG(FORMAT_s_ss_d_d_d_T_f_dn,"%s %3.3s%s %d %d %d T %.1f %d\n")

#define FORMAT_s_W_O_f_TO_s_B_fn 412
ENG(FORMAT_s_W_O_f_TO_s_B_fn,"%s (W:O): %.1f to %s (B:#): %.1f\n")

#define FORMAT_s_ACCEPTED_PAUSE_GAME_CLOCK_PAUSED_n 413
ENG(FORMAT_s_ACCEPTED_PAUSE_GAME_CLOCK_PAUSED_n,"%s accepted pause. Game clock paused.\n")

#define FORMAT_s_AND_s_WOULD_LIKE_TO_PAIR_THEIR_GAME_WITH_YOURS_n 414
ENG(FORMAT_s_AND_s_WOULD_LIKE_TO_PAIR_THEIR_GAME_WITH_YOURS_n,"%s and %s would like to pair their game with yours.\n")

#define FORMAT_s_BMUZZLED_ 415
ENG(FORMAT_s_BMUZZLED_,"%s bmuzzled.")

#define FORMAT_s_CENSORED_n 416
ENG(FORMAT_s_CENSORED_n,"%s censored.\n")

#define FORMAT_s_DECLINES_PAUSE_REQUEST_n 417
ENG(FORMAT_s_DECLINES_PAUSE_REQUEST_n,"%s declines pause request.\n")

#define FORMAT_s_DECLINES_THE_ADJOURN_REQUEST_n 418
ENG(FORMAT_s_DECLINES_THE_ADJOURN_REQUEST_n,"%s declines the adjourn request.\n")

#define FORMAT_s_DECLINES_YOUR_KOMI_OFFER_AND_OFFERS_A_NEW_KOMI_n 419
ENG(FORMAT_s_DECLINES_YOUR_KOMI_OFFER_AND_OFFERS_A_NEW_KOMI_n,"%s declines your komi offer, and offers a new komi:\n")

#define FORMAT_s_DECLINES_YOUR_PAIR_REQUEST_n 420
ENG(FORMAT_s_DECLINES_YOUR_PAIR_REQUEST_n,"%s declines your pair request.\n")

#define FORMAT_s_DECLINES_YOUR_REQUEST_FOR_A_TEACHING_MATCH_n 421
ENG(FORMAT_s_DECLINES_YOUR_REQUEST_FOR_A_TEACHING_MATCH_n,"%s declines your request for a teaching match.\n")

#define FORMAT_s_GMUZZLED_ 422
ENG(FORMAT_s_GMUZZLED_,"%s gmuzzled.")

#define FORMAT_s_HAS_CHANGED_YOUR_EMAIL_ADDRESS_n 423
ENG(FORMAT_s_HAS_CHANGED_YOUR_EMAIL_ADDRESS_n,"%s has changed your email address.\n")

#define FORMAT_s_HAS_CHANGED_YOUR_PASSWORD_n 424
ENG(FORMAT_s_HAS_CHANGED_YOUR_PASSWORD_n,"%s has changed your password.\n")

#define FORMAT_s_HAS_CHANGED_YOUR_REAL_NAME_n 425
ENG(FORMAT_s_HAS_CHANGED_YOUR_REAL_NAME_n,"%s has changed your real name.\n")

#define FORMAT_s_HAS_LOCKED_CHANNEL_dn 426
ENG(FORMAT_s_HAS_LOCKED_CHANNEL_dn,"%s has locked channel %d\n")

#define FORMAT_s_HAS_LOCKED_YOUR_ACCOUNT_n 427
ENG(FORMAT_s_HAS_LOCKED_YOUR_ACCOUNT_n,"%s has locked your account.\n")

#define FORMAT_s_HAS_REMOVED_YOUR_EMAIL_ADDRESS_n 428
ENG(FORMAT_s_HAS_REMOVED_YOUR_EMAIL_ADDRESS_n,"%s has removed your email address.\n")

#define FORMAT_s_HAS_REMOVED_YOUR_REAL_NAME_n 429
ENG(FORMAT_s_HAS_REMOVED_YOUR_REAL_NAME_n,"%s has removed your real name.\n")

#define FORMAT_s_HAS_RESIGNED_THE_GAME_n 430
ENG(FORMAT_s_HAS_RESIGNED_THE_GAME_n,"%s has resigned the game.\n")

#define FORMAT_s_HAS_RESTARTED_YOUR_GAME_n 431
ENG(FORMAT_s_HAS_RESTARTED_YOUR_GAME_n,"%s has restarted your game.\n")

#define FORMAT_s_HAS_RESTORED_YOUR_OLD_GAME_n 432
ENG(FORMAT_s_HAS_RESTORED_YOUR_OLD_GAME_n,"%s has restored your old game.\n")

#define FORMAT_s_HAS_SET_YOUR_WATER_LEVEL_TO_d_n 433
ENG(FORMAT_s_HAS_SET_YOUR_WATER_LEVEL_TO_d_n,"%s has set your water level to %d.\n")

#define FORMAT_s_IS_NOT_RATEDn 434
ENG(FORMAT_s_IS_NOT_RATEDn,"%s is Not Rated\n")

#define FORMAT_s_IS_CONTINUING_A_GAME_WITH_sn 435
ENG(FORMAT_s_IS_CONTINUING_A_GAME_WITH_sn,"%s is continuing a game with %s\n")

#define FORMAT_s_IS_CURRENTLY_PLAYING_A_GAME_ 436
ENG(FORMAT_s_IS_CURRENTLY_PLAYING_A_GAME_,"%s is currently playing a game.")

#define FORMAT_s_IS_NOT_LOGGED_IN_ 437
ENG(FORMAT_s_IS_NOT_LOGGED_IN_,"%s is not logged in.")

#define FORMAT_s_IS_NOT_LOGGED_IN_n 438
ENG(FORMAT_s_IS_NOT_LOGGED_IN_n,"%s is not logged in.\n")

#define FORMAT_s_IS_NOT_RATED_SO_I_CANNOT_SUGGEST_n 439
ENG(FORMAT_s_IS_NOT_RATED_SO_I_CANNOT_SUGGEST_n,"%s is not rated, so I cannot suggest.\n")

#define FORMAT_s_IS_NUMBER_d_IN_PARRAY_OF_SIZE_dn 440
ENG(FORMAT_s_IS_NUMBER_d_IN_PARRAY_OF_SIZE_dn,"%s is number %d in parray of size %d\n")

#define FORMAT_s_ISN_T_LOGGED_IN_ 441
ENG(FORMAT_s_ISN_T_LOGGED_IN_,"%s isn't logged in.")

#define FORMAT_s_MAILED_TO_s_ 442
ENG(FORMAT_s_MAILED_TO_s_,"%s mailed to %s.")

#define FORMAT_s_MUZZLED_ 443
ENG(FORMAT_s_MUZZLED_,"%s muzzled.")

#define FORMAT_s_OFFERS_A_NEW_KOMI_OF_f_n 444
ENG(FORMAT_s_OFFERS_A_NEW_KOMI_OF_f_n,"%s offers a new komi of %.1f.\n")

#define FORMAT_s_REQUESTS_TO_PAUSE_THE_GAME_n 445
ENG(FORMAT_s_REQUESTS_TO_PAUSE_THE_GAME_n,"%s requests to pause the game.\n")

#define FORMAT_s_UNBMUZZLED_ 446
ENG(FORMAT_s_UNBMUZZLED_,"%s unbmuzzled.")

#define FORMAT_s_UNCENSORED_ 447
ENG(FORMAT_s_UNCENSORED_,"%s uncensored.")

#define FORMAT_s_UNCENSORED_n 448
ENG(FORMAT_s_UNCENSORED_n,"%s uncensored.\n")

#define FORMAT_s_UNGMUZZLED_ 449
ENG(FORMAT_s_UNGMUZZLED_,"%s ungmuzzled.")

#define FORMAT_s_UNMUZZLED_ 450
ENG(FORMAT_s_UNMUZZLED_,"%s unmuzzled.")

#define FORMAT_s_UPDATES_THE_KOMI_OFFER_n 451
ENG(FORMAT_s_UPDATES_THE_KOMI_OFFER_n,"%s updates the komi offer.\n")

#define FORMAT_s_WAS_NOT_FINISHED_REMOVING_STONES_PLEASE_TYPE_qDONEq_AGAIN_n 452
ENG(FORMAT_s_WAS_NOT_FINISHED_REMOVING_STONES_PLEASE_TYPE_qDONEq_AGAIN_n,"%s was not finished removing stones.  Please type \"done\" again.\n")

#define FORMAT_s_WITHDRAWS_PAUSE_REQUEST_n 453
ENG(FORMAT_s_WITHDRAWS_PAUSE_REQUEST_n,"%s withdraws pause request.\n")

#define FORMAT_s_WITHDRAWS_THE_ADJOURN_REQUEST_n 454
ENG(FORMAT_s_WITHDRAWS_THE_ADJOURN_REQUEST_n,"%s withdraws the adjourn request.\n")

#define FORMAT_s_WITHDRAWS_THE_MATCH_OFFER_n 455
ENG(FORMAT_s_WITHDRAWS_THE_MATCH_OFFER_n,"%s withdraws the match offer.\n")

#define FORMAT_s_WITHDRAWS_THE_PAIR_REQUEST_n 456
ENG(FORMAT_s_WITHDRAWS_THE_PAIR_REQUEST_n,"%s withdraws the pair request.\n")

#define FORMAT_s_WITHDRAWS_THE_TEACHING_MATCH_OFFER_n 457
ENG(FORMAT_s_WITHDRAWS_THE_TEACHING_MATCH_OFFER_n,"%s withdraws the teaching match offer.\n")

#define FORMAT_ss 458
ENG(FORMAT_ss,"%s%s")

#define FORMAT_s_s_s 459
ENG(FORMAT_s_s_s,"%s: %-10s %-6s")

#define FORMAT_s_s_d_RATED_GAMES_dn 460
ENG(FORMAT_s_s_d_RATED_GAMES_dn,"%s: %s (%d) Rated Games: %d\n")

#define FORMAT_s_sc_f_n 461
ENG(FORMAT_s_sc_f_n,"%s: %s%c (%.2f)\n")

#define FORMAT_s_COMMAND_FAILED_n 462
ENG(FORMAT_s_COMMAND_FAILED_n,"%s: Command failed.\n")

#define FORMAT_s_INACCESSIBLE_COMMAND_n 463
ENG(FORMAT_s_INACCESSIBLE_COMMAND_n,"%s: Inaccessible command.\n")

#define FORMAT_sYOU_WITHDRAW_THE_MATCH_OFFER_TO_s_n 464
ENG(FORMAT_sYOU_WITHDRAW_THE_MATCH_OFFER_TO_s_n,"You withdraw the match offer to %s.\n")

#define FORMAT_d_s 465
ENG(FORMAT_d_s,"(%d)%s")

#define FORMAT_CHINESE_WHITE_f_BLACK_fn 466
ENG(FORMAT_CHINESE_WHITE_f_BLACK_fn,"(Chinese) White: %.1f, Black: %.1f\n")

#define FORMAT_JAPANESE_WHITE_f_BLACK_fn 467
ENG(FORMAT_JAPANESE_WHITE_f_BLACK_fn,"(Japanese) White: %.1f, Black: %.1f\n")

#define FORMAT_NONE_ 468
ENG(FORMAT_NONE_,"(none)")

#define FORMAT_NEW_PLAYER_s_s_BY_ss_n 469
ENG(FORMAT_NEW_PLAYER_s_s_BY_ss_n,"*** New Player: %s [%s] by %s%s\n")

#define FORMAT_YOU_HAVE_NO_PASSWORD_PLEASE_SET_ONE_WITH_THE_PASSWORD_COMMAND_n 470
ENG(FORMAT_YOU_HAVE_NO_PASSWORD_PLEASE_SET_ONE_WITH_THE_PASSWORD_COMMAND_n,"*** You have no password. Please set one with the password command.\n")

#define FORMAT_QTELL_s_0_ 471
ENG(FORMAT_QTELL_s_0_,"*qtell %s 0*")

#define FORMAT_QTELL_s_1_ 472
ENG(FORMAT_QTELL_s_1_,"*qtell %s 1*")

#define FORMAT_YOUR_NOTIFY_LIST_CONTAINS_d_NAMES_n 473
ENG(FORMAT_YOUR_NOTIFY_LIST_CONTAINS_d_NAMES_n,"-- Your notify list contains %d names: --\n")

#define FORMAT_ssn 474
ENG(FORMAT_ssn,"--> %s%s\n")

#define FORMAT_s_s 475
ENG(FORMAT_s_s,">%s<%s")

#define FORMAT_A_PLAYER_BY_THAT_NAME_IS_LOGGED_IN_ 476
ENG(FORMAT_A_PLAYER_BY_THAT_NAME_IS_LOGGED_IN_,"A player by that name is logged in.")

#define FORMAT_A_PLAYER_BY_THAT_NAME_IS_LOGGED_IN_n 477
ENG(FORMAT_A_PLAYER_BY_THAT_NAME_IS_LOGGED_IN_n,"A player by that name is logged in.\n")

#define FORMAT_A_PLAYER_BY_THAT_NEW_NAME_IS_LOGGED_IN_ 478
ENG(FORMAT_A_PLAYER_BY_THAT_NEW_NAME_IS_LOGGED_IN_,"A player by that new name is logged in.")

#define FORMAT_A_PLAYER_BY_THE_NAME_s_IS_ALREADY_REGISTERED_ 479
ENG(FORMAT_A_PLAYER_BY_THE_NAME_s_IS_ALREADY_REGISTERED_,"A player by the name %s is already registered.")

#define FORMAT_A_PLAYER_BY_THE_NAME_s_IS_ALREADY_REGISTERED_n 480
ENG(FORMAT_A_PLAYER_BY_THE_NAME_s_IS_ALREADY_REGISTERED_n,"A player by the name %s is already registered.\n")

#define FORMAT_ACCOUNT_s_LOCKED_n 481
ENG(FORMAT_ACCOUNT_s_LOCKED_n,"Account %s locked!\n")

#define FORMAT_ADDRESS_s_LAST_CON_FROM_s_n 482
ENG(FORMAT_ADDRESS_s_LAST_CON_FROM_s_n,"Address: %s [Last Con From: %s]\n")

#define FORMAT_ADDRESS_s_n 483
ENG(FORMAT_ADDRESS_s_n,"Address: %s \n")

#define FORMAT_ADMIN_LEVEL_ADMINISTRATOR_n 484
ENG(FORMAT_ADMIN_LEVEL_ADMINISTRATOR_n,"Admin Level: Administrator.\n")

#define FORMAT_ADMIN_LEVEL_OF_s_SET_TO_d_n 485
ENG(FORMAT_ADMIN_LEVEL_OF_s_SET_TO_d_n,"Admin level of %s set to %d.\n")

#define FORMAT_ADMINS_AVAILABLE_TO_HELP_YOU_n 486
ENG(FORMAT_ADMINS_AVAILABLE_TO_HELP_YOU_n,"Admins available to help you:\n")

#define FORMAT_ALIAS_REMOVED_ 487
ENG(FORMAT_ALIAS_REMOVED_,"Alias removed.")

#define FORMAT_ALIAS_REPLACED_n 488
ENG(FORMAT_ALIAS_REPLACED_n,"Alias replaced.\n")

#define FORMAT_ALIAS_SET_n 489
ENG(FORMAT_ALIAS_SET_n,"Alias set.\n")

#define FORMAT_ALL_RATED_LOSSES_dn 490
ENG(FORMAT_ALL_RATED_LOSSES_dn,"All rated losses: %d\n")

#define FORMAT_ALL_RATED_WINS_dn 491
ENG(FORMAT_ALL_RATED_WINS_dn,"All rated wins: %d\n")

#define FORMAT_AN_UNKNOWN_ERROR_OCCURED_OPENING_THE_RATINGS_FILE_n 492
ENG(FORMAT_AN_UNKNOWN_ERROR_OCCURED_OPENING_THE_RATINGS_FILE_n,"An unknown error occured opening the ratings file.\n")

#define FORMAT_AN_UNKNOWN_ERROR_OCCUREDn 493
ENG(FORMAT_AN_UNKNOWN_ERROR_OCCUREDn,"An unknown error occured\n")

#define FORMAT_AN_UNLUCKY_CHANCE_OCCUREDn 494
ENG(FORMAT_AN_UNLUCKY_CHANCE_OCCUREDn,"An unlucky chance occured\n")

#define FORMAT_ASETHANDLE_FAILED_ 495
ENG(FORMAT_ASETHANDLE_FAILED_,"Asethandle failed.")

#define FORMAT_BMUZZLED_PLAYERS_n 496
ENG(FORMAT_BMUZZLED_PLAYERS_n,"BMuzzled players:\n")

#define FORMAT_BLACK_HAS_ONLY_d_SECONDS_LEFT_n 497
ENG(FORMAT_BLACK_HAS_ONLY_d_SECONDS_LEFT_n,"Black has only %d seconds left.\n")

#define FORMAT_BLACK_s_d_d 498
ENG(FORMAT_BLACK_s_d_d,"Black(%s) : %d:%02d")

#define FORMAT_BOARDSIZE_MUST_BE_2n 499
ENG(FORMAT_BOARDSIZE_MUST_BE_2n,"Boardsize must be >= 2\n")

#define FORMAT_BOTH_PLAYERS_HAVE_LESS_THAN_d_SECONDS_LEFT_n 500
ENG(FORMAT_BOTH_PLAYERS_HAVE_LESS_THAN_d_SECONDS_LEFT_n,"Both players have less than %d seconds left.\n")

#define FORMAT_BYO_YOMI_TIME_MUST_BE_0n 501
ENG(FORMAT_BYO_YOMI_TIME_MUST_BE_0n,"Byo-yomi time must be >= 0\n")

#define FORMAT_BYTES_SENT_dn 502
ENG(FORMAT_BYTES_SENT_dn,"Bytes sent: %ld\n")

#define FORMAT_CAN_T_RAISE_UNTIL_THEY_LEAVE_ 503
ENG(FORMAT_CAN_T_RAISE_UNTIL_THEY_LEAVE_,"Can't raise until they leave.")

#define FORMAT_CANNOT_TYPE_DONE_UNTIL_BOTH_SIDES_HAVE_PASSED_ 504
ENG(FORMAT_CANNOT_TYPE_DONE_UNTIL_BOTH_SIDES_HAVE_PASSED_,"Cannot type done until both sides have passed.")

#define FORMAT_CHANGED_s_S_LOGIN_TO_s_n 506
ENG(FORMAT_CHANGED_s_S_LOGIN_TO_s_n,"Changed %s's login to %s.\n")

#define FORMAT_CHANNEL_d_TITLE_n 507
ENG(FORMAT_CHANNEL_d_TITLE_n,"Channel %d Title:\n")
GER(FORMAT_CHANNEL_d_TITLE_n,"Kanal %d Titel:\n")

#define FORMAT_CHANNEL_d_TOPIC_sn 508
ENG(FORMAT_CHANNEL_d_TOPIC_sn,"Channel %d Topic: %s\n")

#define FORMAT_CHANNEL_d_sss_sn 509
ENG(FORMAT_CHANNEL_d_sss_sn,"Channel %d: (%s%s%s) %s\n")
GER(FORMAT_CHANNEL_d_sss_sn,"Kanal %d: (%s%s%s) %s\n")

#define FORMAT_CLIENT_TYPE_CHANGED_TO_ 510
ENG(FORMAT_CLIENT_TYPE_CHANGED_TO_,"Client type changed to: ")

#define FORMAT_CODE_COMMANDn 511
ENG(FORMAT_CODE_COMMANDn,"Code Command\n")

#define FORMAT_CONFIDENCE_f_PERCENTn 513
ENG(FORMAT_CONFIDENCE_f_PERCENTn,"Confidence = %.2f percent\n")

#define FORMAT_CONNECTED_TO_sn 514
ENG(FORMAT_CONNECTED_TO_sn,"Connected to: %s\n")

#define FORMAT_CREATING_MATCH_d_WITH_s_n 515
ENG(FORMAT_CREATING_MATCH_d_WITH_s_n,"Creating match [%d] with %s.\n")

#define FORMAT_CURRENT_LOCAL_TIME_sn 516
ENG(FORMAT_CURRENT_LOCAL_TIME_sn,"Current local time:  %s\n")

#define FORMAT_CURRENT_TIME_GMT_sn 517
ENG(FORMAT_CURRENT_TIME_GMT_sn,"Current time (GMT):  %s\n")

#define FORMAT_DEBUG_LEVEL_SET_TO_d 518
ENG(FORMAT_DEBUG_LEVEL_SET_TO_d,"Debug level set to: %d")

#define FORMAT_DECLINING_KOMI_OFFER_FROM_s_AND_OFFERING_NEW_KOMI_n 519
ENG(FORMAT_DECLINING_KOMI_OFFER_FROM_s_AND_OFFERING_NEW_KOMI_n,"Declining komi offer from %s and offering new komi.\n")

#define FORMAT_DEFAULT_BYO_YOMI_STONES_SET_TO_d_n 520
ENG(FORMAT_DEFAULT_BYO_YOMI_STONES_SET_TO_d_n,"Default byo yomi stones set to %d.\n")

#define FORMAT_DEFAULT_BYO_YOMI_TIME_SET_TO_d_n 521
ENG(FORMAT_DEFAULT_BYO_YOMI_TIME_SET_TO_d_n,"Default byo yomi time set to %d.\n")

#define FORMAT_DEFAULT_CHANNEL_SET_TO_d_n 522
ENG(FORMAT_DEFAULT_CHANNEL_SET_TO_d_n,"Default channel set to %d.\n")

#define FORMAT_DEFAULT_SIZE_SET_TO_d_n 523
ENG(FORMAT_DEFAULT_SIZE_SET_TO_d_n,"Default size set to %d.\n")

#define FORMAT_DEFAULT_TIME_SET_TO_d_n 524
ENG(FORMAT_DEFAULT_TIME_SET_TO_d_n,"Default time set to %d.\n")

#define FORMAT_ERROR_A_PLAYER_NAMED_s_WAS_EXPECTED_BUT_NOT_FOUND_n 525
ENG(FORMAT_ERROR_A_PLAYER_NAMED_s_WAS_EXPECTED_BUT_NOT_FOUND_n,"ERROR: a player named %s was expected but not found!\n")

#define FORMAT_EITHER_THE_FILE_WAS_NOT_FOUND_OR_YOUR_EMAIL_ADDRESS_IS_INVALID_ 526
ENG(FORMAT_EITHER_THE_FILE_WAS_NOT_FOUND_OR_YOUR_EMAIL_ADDRESS_IS_INVALID_,"Either the file was not found, or your email address is invalid.")

#define FORMAT_EMAIL_ADDRESS_FOR_s_REMOVEDn 527
ENG(FORMAT_EMAIL_ADDRESS_FOR_s_REMOVEDn,"Email address for %s removed\n")

#define FORMAT_EMAIL_ADDRESS_OF_s_CHANGED_TO_qsq_ 528
ENG(FORMAT_EMAIL_ADDRESS_OF_s_CHANGED_TO_qsq_,"Email address of %s changed to \"%s\".")

#define FORMAT_ERROR_NOTE_NOT_SAVEDn 529
ENG(FORMAT_ERROR_NOTE_NOT_SAVEDn,"Error.  Note not saved\n")

#define FORMAT_EVENT_INFO_s_DELETED 530

#define FORMAT_FILE_NOT_FOUND_n 531
ENG(FORMAT_FILE_NOT_FOUND_n,"File not found!\n")

#define FORMAT_FILEn 532
ENG(FORMAT_FILEn,"File\n")

#define FORMAT_FIRST_CREATE_A_MATCH_AGAINST_YOUR_OPPONENT_s_n 533
ENG(FORMAT_FIRST_CREATE_A_MATCH_AGAINST_YOUR_OPPONENT_s_n,"First, create a match against your opponent, %s.\n")

#define FORMAT_FOR_9X9_PLAY_AN_EVEN_GAME_AND_SET_KOMI_TO_f_n 534
ENG(FORMAT_FOR_9X9_PLAY_AN_EVEN_GAME_AND_SET_KOMI_TO_f_n,"For   9x9:  Play an even game and set komi to %.1f.\n")

#define FORMAT_FOUND_d_FILES_MATCHING_THAT_n 535
ENG(FORMAT_FOUND_d_FILES_MATCHING_THAT_n,"Found %d files matching that.\n")

#define FORMAT_FOUND_d_NAMES_ 536
ENG(FORMAT_FOUND_d_NAMES_,"Found %d names.")

#define FORMAT_FOUND_d_OBSERVERS_ 537
ENG(FORMAT_FOUND_d_OBSERVERS_,"Found %d observers.")

#define FORMAT_FOUND_d_STORED_GAMES_n 538
ENG(FORMAT_FOUND_d_STORED_GAMES_n,"Found %d stored games.\n")


#define FORMAT_FULL_NAME_SET_TO_s_n 540
ENG(FORMAT_FULL_NAME_SET_TO_s_n,"Full Name set to %s.\n")

#define FORMAT_GMUZZLED_PLAYERS_n 541
ENG(FORMAT_GMUZZLED_PLAYERS_n,"GMuzzled players:\n")

#define FORMAT_GAME_d_I_s_ss_VS_s_ss_n 542
ENG(FORMAT_GAME_d_I_s_ss_VS_s_ss_n,"Game %d (I): %s [%3.3s%s] vs %s [%3.3s%s]\n")

#define FORMAT_GAME_d_s_VS_s_HAS_ADJOURNED_n 543
ENG(FORMAT_GAME_d_s_VS_s_HAS_ADJOURNED_n,"Game %d: %s vs %s : has adjourned.\n")

#define FORMAT_GAME_dn 544
ENG(FORMAT_GAME_dn,"Game : %d\n")

#define FORMAT_GAME_ALREADY_COUNTS_TOWARD_RATINGS_n 545
ENG(FORMAT_GAME_ALREADY_COUNTS_TOWARD_RATINGS_n,"Game already counts toward ratings.\n")

#define FORMAT_GAME_CLOCK_IS_PAUSED_USE_qUNPAUSEq_TO_RESUME_n 546
ENG(FORMAT_GAME_CLOCK_IS_PAUSED_USE_qUNPAUSEq_TO_RESUME_n,"Game clock is paused, use \"unpause\" to resume.\n")

#define FORMAT_GAME_CLOCK_PAUSED_USE_qUNPAUSEq_TO_RESUME_n 547
ENG(FORMAT_GAME_CLOCK_PAUSED_USE_qUNPAUSEq_TO_RESUME_n,"Game clock paused. Use \"unpause\" to resume.\n")

#define FORMAT_GAME_CLOCK_PAUSED_n 548
ENG(FORMAT_GAME_CLOCK_PAUSED_n,"Game clock paused.\n")

#define FORMAT_GAME_CLOCK_RESUMED_ 549
ENG(FORMAT_GAME_CLOCK_RESUMED_,"Game clock resumed.")

#define FORMAT_GAME_CLOCK_RESUMED_n 550
ENG(FORMAT_GAME_CLOCK_RESUMED_n,"Game clock resumed.\n")

#define FORMAT_GAME_FAILED_TO_LOAD_ 551
ENG(FORMAT_GAME_FAILED_TO_LOAD_,"Game failed to load.")

#define FORMAT_GAME_HAS_BEEN_ADJOURNED_n 552
ENG(FORMAT_GAME_HAS_BEEN_ADJOURNED_n,"Game has been adjourned.\n")

#define FORMAT_GAME_IS_ALREADY_FREE_USE_qUNFREEq_TO_CHANGE_THIS_n 553
ENG(FORMAT_GAME_IS_ALREADY_FREE_USE_qUNFREEq_TO_CHANGE_THIS_n,"Game is already free. (Use \"unfree\" to change this.)\n")

#define FORMAT_GAME_IS_ALREADY_PAUSED_USE_qUNPAUSEq_TO_RESUME_ 554
ENG(FORMAT_GAME_IS_ALREADY_PAUSED_USE_qUNPAUSEq_TO_RESUME_,"Game is already paused, use \"unpause\" to resume.")

#define FORMAT_GAME_IS_NOT_PAUSED_ 555
ENG(FORMAT_GAME_IS_NOT_PAUSED_,"Game is not paused.")

#define FORMAT_GAME_SAVED_ 556
ENG(FORMAT_GAME_SAVED_,"Game saved.")

#define FORMAT_GAME_WILL_NOT_COUNT_TOWARD_RATINGS_n 557
ENG(FORMAT_GAME_WILL_NOT_COUNT_TOWARD_RATINGS_n,"Game will NOT count toward ratings.\n")

#define FORMAT_GAME_WILL_COUNT_TOWARD_RATINGS_n 558
ENG(FORMAT_GAME_WILL_COUNT_TOWARD_RATINGS_n,"Game will count toward ratings.\n")

#define FORMAT_GAME_GO_1_n 559
ENG(FORMAT_GAME_GO_1_n,"Game:        go (1)\n")

#define FORMAT_GAMES_d_AND_d_ARE_NOW_PAIRED_n 560
ENG(FORMAT_GAMES_d_AND_d_ARE_NOW_PAIRED_n,"Games %d and %d are now paired.\n")

#define FORMAT_GOOD_LUCK_n 561
ENG(FORMAT_GOOD_LUCK_n,"Good Luck!\n")

#define FORMAT_HEIGHT_SET_TO_d_n 562
ENG(FORMAT_HEIGHT_SET_TO_d_n,"Height set to %d.\n")

#define FORMAT_HIGH_CONFIDENCE_LOSSES_dn 563
ENG(FORMAT_HIGH_CONFIDENCE_LOSSES_dn,"High confidence losses: %d\n")

#define FORMAT_HIGH_CONFIDENCE_WINS_dn 564
ENG(FORMAT_HIGH_CONFIDENCE_WINS_dn,"High confidence wins: %d\n")

#define FORMAT_I_DON_T_KNOW_WHAT_YOU_MEAN_BY_sn 565
ENG(FORMAT_I_DON_T_KNOW_WHAT_YOU_MEAN_BY_sn,"I don't know what you mean by %s\n")

#define FORMAT_I_SUGGEST_THAT_s_PLAY_s_AGAINST_s_n 566
ENG(FORMAT_I_SUGGEST_THAT_s_PLAY_s_AGAINST_s_n,"I suggest that %s play %s against %s:\n")

#define FORMAT_IF_YOU_WISH_TO_ACCEPT_ANY_OF_THESE_OFFERS_TYPE_ACCEPT_N_nOR_JUST_ACCEPT_IF_THERE_IS_ONLY_ONE_OFFER_n 567
ENG(FORMAT_IF_YOU_WISH_TO_ACCEPT_ANY_OF_THESE_OFFERS_TYPE_ACCEPT_N_nOR_JUST_ACCEPT_IF_THERE_IS_ONLY_ONE_OFFER_n,"If you wish to accept any of these offers type 'accept n'\nor just 'accept' if there is only one offer.\n")

#define FORMAT_ILLEGAL_MOVE_n 568
ENG(FORMAT_ILLEGAL_MOVE_n,"Illegal Move!\n")

#define FORMAT_ILLEGAL_CHARACTERS_IN_PLAYER_NAME_ONLY_A_Z_A_Z_0_9_ALLOWED_ 569
ENG(FORMAT_ILLEGAL_CHARACTERS_IN_PLAYER_NAME_ONLY_A_Z_A_Z_0_9_ALLOWED_,"Illegal characters in player name. Only A-Z a-z 0-9 allowed.")

#define FORMAT_IMPORTANT_TO_REMEMBER_DO_NOT_MOVE_UNTIL_AFTER_THE_GAMES_ARE_PAIRED_n 570
ENG(FORMAT_IMPORTANT_TO_REMEMBER_DO_NOT_MOVE_UNTIL_AFTER_THE_GAMES_ARE_PAIRED_n,"Important to remember: do not move until after the games are paired.\n")

#define FORMAT_INVALID_EMAIL_ADDRESSn 571
ENG(FORMAT_INVALID_EMAIL_ADDRESSn,"Invalid email address\n")

#define FORMAT_IT_IS_NOT_YOUR_MOVE_n 572
ENG(FORMAT_IT_IS_NOT_YOUR_MOVE_n,"It is not your move.\n")

#define FORMAT_IT_WOULD_NOT_BE_NICE_TO_REGISTER_GUEST 573
ENG(FORMAT_IT_WOULD_NOT_BE_NICE_TO_REGISTER_GUEST,"It would not be nice to register guest")

#define FORMAT_KIBITZ_s_ss_GAME_s_VS_s_d_n 574
ENG(FORMAT_KIBITZ_s_ss_GAME_s_VS_s_d_n,"Kibitz %s [%3.3s%s]: Game %s vs %s [%d]\n")

#define FORMAT_KOMI_IS_NOW_SET_TO_fn 575
ENG(FORMAT_KOMI_IS_NOW_SET_TO_fn,"Komi is now set to %.1f\n")

#define FORMAT_KOMI_SET_TO_f_IN_MATCH_dn 579
ENG(FORMAT_KOMI_SET_TO_f_IN_MATCH_dn,"Komi set to %.1f in match %d\n")

#define FORMAT_LAST_ACCESS_LOCAL_NOT_ON_sn 581
ENG(FORMAT_LAST_ACCESS_LOCAL_NOT_ON_sn,"Last Access(local): (Not on)    %s\n")

#define FORMAT_LAST_IN_CHANNEL_d_n 582
ENG(FORMAT_LAST_IN_CHANNEL_d_n,"Last in channel %d:\n")

#define FORMAT_LOGGING_YOU_OUT_n 583
ENG(FORMAT_LOGGING_YOU_OUT_n,"Logging you out.\n")

#define FORMAT_LOSSES_dn 584
ENG(FORMAT_LOSSES_dn,"Losses:       %d\n")

#define FORMAT_MATCH_d_WITH_s_IN_d_ACCEPTED_n 585
ENG(FORMAT_MATCH_d_WITH_s_IN_d_ACCEPTED_n,"Match [%d] with %s in %d accepted.\n")

#define FORMAT_MATCHES_THE_FOLLOWING_PLAYER_S_n 586
ENG(FORMAT_MATCHES_THE_FOLLOWING_PLAYER_S_n,"Matches the following player(s):\n")

#define FORMAT_MESSAGES_n 587
ENG(FORMAT_MESSAGES_n,"Messages:\n")
GER(FORMAT_MESSAGES_n,"Botschaften:\n")

#define FORMAT_MUZZLED_PLAYERS_n 588
ENG(FORMAT_MUZZLED_PLAYERS_n,"Muzzled players:\n")

#define FORMAT_MUZZLED_s_GMUZZLED_s_BMUZZLED_s_TMUZZLED_s_KMUZZLED_s 589
ENG(FORMAT_MUZZLED_s_GMUZZLED_s_BMUZZLED_s_TMUZZLED_s_KMUZZLED_s,"Muzzled: %s  GMuzzled: %s  BMuzzled: %s  TMuzzled: %s  KMuzzled: %s")

#define FORMAT_NEWS_s_s_nn_sn 590
ENG(FORMAT_NEWS_s_s_nn_sn,"NEWS %3s (%s)\n\n         %s\n")

#define FORMAT_NOT_SILENT 591
ENG(FORMAT_NOT_SILENT,"NOT SILENT")

#define FORMAT_NO_NAME_GO_SERVER_NNGS_VERSION_sn 592
ENG(FORMAT_NO_NAME_GO_SERVER_NNGS_VERSION_sn,"No Name Go Server (NNGS) version %s\n")

#define FORMAT_NO_LAST_CHANNEL_ 593
ENG(FORMAT_NO_LAST_CHANNEL_,"No last channel.")

#define FORMAT_NO_ONE_WAS_UNCENSORED_ 594
ENG(FORMAT_NO_ONE_WAS_UNCENSORED_,"No one was uncensored.")

#define FORMAT_NO_PLAYER_BY_THE_NAME_s_IS_REGISTERED_ 595
ENG(FORMAT_NO_PLAYER_BY_THE_NAME_s_IS_REGISTERED_,"No player by the name %s is registered.")

#define FORMAT_NO_PLAYER_NAMED_s_IS_LOGGED_IN_ 596
ENG(FORMAT_NO_PLAYER_NAMED_s_IS_LOGGED_IN_,"No player named %s is logged in.")

#define FORMAT_NO_RATING_INFORMATION_FOR_sn 597
ENG(FORMAT_NO_RATING_INFORMATION_FOR_sn,"No rating information for %s\n")

#define FORMAT_NO_SUCH_CHANNEL_ 598
ENG(FORMAT_NO_SUCH_CHANNEL_,"No such channel.")

#define FORMAT_NO_SUCH_GAME_ 599
ENG(FORMAT_NO_SUCH_GAME_,"No such game.")

#define FORMAT_NO_SUCH_LADDER_PLAYER 600
ENG(FORMAT_NO_SUCH_LADDER_PLAYER,"No such ladder player")

#define FORMAT_NO_SUCH_LADDER 601
ENG(FORMAT_NO_SUCH_LADDER,"No such ladder")

#define FORMAT_NO_YOU_DON_T_ 602
ENG(FORMAT_NO_YOU_DON_T_,"No, you don't.")

#define FORMAT_NUKING_s 603
ENG(FORMAT_NUKING_s,"Nuking: %s")

#define FORMAT_OBSERVING_GAME_d_s_VS_s_ 604
ENG(FORMAT_OBSERVING_GAME_d_s_VS_s_,"Observing game %2d (%s vs. %s) :")

#define FORMAT_OBTAIN_A_NEW_HANDLE_FOR_THE_DEAD_PERSON_n 605
ENG(FORMAT_OBTAIN_A_NEW_HANDLE_FOR_THE_DEAD_PERSON_n,"Obtain a new handle for the dead person.\n")

#define FORMAT_OBTAIN_ANOTHER_NEW_HANDLE_FOR_THE_DEAD_PERSON_ 606
ENG(FORMAT_OBTAIN_ANOTHER_NEW_HANDLE_FOR_THE_DEAD_PERSON_,"Obtain another new handle for the dead person.")

#define FORMAT_OFFERING_A_KOMI_OF_f_TO_s 607
ENG(FORMAT_OFFERING_A_KOMI_OF_f_TO_s,"Offering a komi of %.1f to %s")

#define FORMAT_ONCE_YOUR_MATCH_IS_CREATED_CHECK_TO_SEE_WHAT_s_S_GAME_NUMBER_IS_n 608
ENG(FORMAT_ONCE_YOUR_MATCH_IS_CREATED_CHECK_TO_SEE_WHAT_s_S_GAME_NUMBER_IS_n,"Once your match is created, check to see what %s's game number is.\n")

#define FORMAT_ONE_OF_THE_4_LISTED_PLAYERS_IS_ALREADY_PLAYING_A_GAME_ 609
ENG(FORMAT_ONE_OF_THE_4_LISTED_PLAYERS_IS_ALREADY_PLAYING_A_GAME_,"One of the 4 listed players is already playing a game.")

#define FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_s_COMMAND_ 610
ENG(FORMAT_ONLY_REGISTERED_PLAYERS_CAN_USE_THE_s_COMMAND_,"Only registered players can use the %s command.")

#define FORMAT_ONLY_THE_WHITE_PLAYER_MAY_REQUEST_A_PAIR_MATCH 611
ENG(FORMAT_ONLY_THE_WHITE_PLAYER_MAY_REQUEST_A_PAIR_MATCH,"Only the White player may request a Pair match")

#define FORMAT_ONLY_VALID_AS_YOUR_FIRST_MOVE_ 612
ENG(FORMAT_ONLY_VALID_AS_YOUR_FIRST_MOVE_,"Only valid as your first move.")

#define FORMAT_PONG 613
ENG(FORMAT_PONG,"PONG")

#define FORMAT_PASS_IS_NOT_VALID_DURING_SCORING_n 614
ENG(FORMAT_PASS_IS_NOT_VALID_DURING_SCORING_n,"Pass is not valid during scoring.\n")

#define FORMAT_PASSWORD_OF_s_CHANGED_TO_qsq_n 615
ENG(FORMAT_PASSWORD_OF_s_CHANGED_TO_qsq_n,"Password of %s changed to \"%s\".\n")

#define FORMAT_PAUSE_REQUEST_SENT_n 616
ENG(FORMAT_PAUSE_REQUEST_SENT_n,"Pause request sent.\n")

#define FORMAT_PLAYER_s_HAS_NO_SGF_GAMES_ 617
ENG(FORMAT_PLAYER_s_HAS_NO_SGF_GAMES_,"Player %s has no SGF games.")

#define FORMAT_PLAYER_s_RAISED_FROM_DEAD_ 618
ENG(FORMAT_PLAYER_s_RAISED_FROM_DEAD_,"Player %s raised from dead.")

#define FORMAT_PLAYER_s_REINCARNATED_TO_s_ 619
ENG(FORMAT_PLAYER_s_REINCARNATED_TO_s_,"Player %s reincarnated to %s.")

#define FORMAT_PLAYER_s_REMOVED_ 620
ENG(FORMAT_PLAYER_s_REMOVED_,"Player %s removed.")

#define FORMAT_PLAYER_s_RENAMED_TO_s_ 621
ENG(FORMAT_PLAYER_s_RENAMED_TO_s_,"Player %s renamed to %s.")

#define FORMAT_PLAYER_qsq_IS_CENSORING_YOU_n 622
ENG(FORMAT_PLAYER_qsq_IS_CENSORING_YOU_n,"Player \"%s\" is censoring you.\n")

#define FORMAT_PLAYER_qsq_IS_INVOLVED_IN_ANOTHER_GAME_n 623
ENG(FORMAT_PLAYER_qsq_IS_INVOLVED_IN_ANOTHER_GAME_n,"Player \"%s\" is involved in another game.\n")

#define FORMAT_PLAYER_qsq_IS_NOT_OPEN_TO_MATCH_REQUESTS_n 624
ENG(FORMAT_PLAYER_qsq_IS_NOT_OPEN_TO_MATCH_REQUESTS_n,"Player \"%s\" is not open to match requests.\n")

#define FORMAT_PLAYER_NAME_IS_TOO_LONG 625
ENG(FORMAT_PLAYER_NAME_IS_TOO_LONG,"Player name is too long")

#define FORMAT_PLAYER_NAME_IS_TOO_SHORT 626
ENG(FORMAT_PLAYER_NAME_IS_TOO_SHORT,"Player name is too short")

#define FORMAT_PLAYER_sn 627
ENG(FORMAT_PLAYER_sn,"Player:      %s\n")

#define FORMAT_PLEASE_EXPLAIN_YOUR_RANK_BY_USING_qSET_RANKq_ 628
ENG(FORMAT_PLEASE_EXPLAIN_YOUR_RANK_BY_USING_qSET_RANKq_,"Please explain your rank by using \"set rank\".")

#define FORMAT_PLEASE_SEE_qHELP_REGISTERqn 630
ENG(FORMAT_PLEASE_SEE_qHELP_REGISTERqn,"Please see \"Help register\"\n")

#define FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_DECLINE_n 631
ENG(FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_DECLINE_n,"Please specify which one you wish to decline.\n")

#define FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_WITHDRAW_n 632
ENG(FORMAT_PLEASE_SPECIFY_WHICH_ONE_YOU_WISH_TO_WITHDRAW_n,"Please specify which one you wish to withdraw.\n")

#define FORMAT_PROBLEM_WITH_19X19_LADDER_STRUCTURE_n 634
ENG(FORMAT_PROBLEM_WITH_19X19_LADDER_STRUCTURE_n,"Problem with 19x19 ladder structure!\n")

#define FORMAT_PROBLEM_WITH_9X9_LADDER_STRUCTURE_n 635
ENG(FORMAT_PROBLEM_WITH_9X9_LADDER_STRUCTURE_n,"Problem with 9x9 ladder structure!\n")

#define FORMAT_PROBLEM_THERE_IS_NO_PROBLEM_d_HMMMMM_ 636
ENG(FORMAT_PROBLEM_THERE_IS_NO_PROBLEM_d_HMMMMM_,"Problem: there is no problem %d (Hmmmmm)")

#define FORMAT_RAISEDEAD_FAILED_ 637
ENG(FORMAT_RAISEDEAD_FAILED_,"Raisedead failed.")

#define FORMAT_RANGE_f_STONES_f_f_sn 638
ENG(FORMAT_RANGE_f_STONES_f_f_sn,"Range: %.2f stones (%.2f %+.2f), %s\n")

#define FORMAT_RANK_SET_TO_s_n 639
ENG(FORMAT_RANK_SET_TO_s_n,"Rank set to %s.\n")

#define FORMAT_RANK_s_dn 640
ENG(FORMAT_RANK_s_dn,"Rank:  %s   %d\n")

#define FORMAT_RATED_GAMES_dn 641
ENG(FORMAT_RATED_GAMES_dn,"Rated Games:      %d\n")

#define FORMAT_RATED_GAMES_u_u_WINS_u_LOSSES_n 642
ENG(FORMAT_RATED_GAMES_u_u_WINS_u_LOSSES_n,"Rated games: %u (%u wins, %u losses)\n")

#define FORMAT_RATED_LOSSES_VERSUS_RATED_PLAYERS_dn 643
ENG(FORMAT_RATED_LOSSES_VERSUS_RATED_PLAYERS_dn,"Rated losses versus rated players: %d\n")

#define FORMAT_RATED_WINS_VERSUS_RATED_PLAYERS_dn 644
ENG(FORMAT_RATED_WINS_VERSUS_RATED_PLAYERS_dn,"Rated wins versus rated players: %d\n")

#define FORMAT_RATING_ss_dn 645
ENG(FORMAT_RATING_ss_dn,"Rating:     %3.3s%s   %d\n")

#define FORMAT_RATINGS_UPDATE_YOU_ARE_NOW_ss_d_WERE_d_d_RATED_GAMES_n 646
ENG(FORMAT_RATINGS_UPDATE_YOU_ARE_NOW_ss_d_WERE_d_d_RATED_GAMES_n,"Ratings update: You are now %s%s (%d (were %d), %d rated games)\n")

#define FORMAT_RATINGS_UPDATE_YOU_ARE_NOW_ss_d_d_RATED_GAMES_n 647
ENG(FORMAT_RATINGS_UPDATE_YOU_ARE_NOW_ss_d_d_RATED_GAMES_n,"Ratings update: You are now %s%s (%d), %d rated games)\n")

#define FORMAT_RATINGS_UPDATE_YOU_ARE_STILL_ss_d_d_RATED_GAMES_n 648
ENG(FORMAT_RATINGS_UPDATE_YOU_ARE_STILL_ss_d_d_RATED_GAMES_n,"Ratings update: You are still %s%s (%d, %d rated games)\n")

#define FORMAT_REAL_NAME_FOR_s_REMOVED 649
ENG(FORMAT_REAL_NAME_FOR_s_REMOVED,"Real name for %s removed")

#define FORMAT_REAL_NAME_OF_s_CHANGED_TO_qsq_ 650
ENG(FORMAT_REAL_NAME_OF_s_CHANGED_TO_qsq_,"Real name of %s changed to \"%s\".")

#define FORMAT_REMOVING_sn 651
ENG(FORMAT_REMOVING_sn,"Removing @ %s\n")

#define FORMAT_REMOVING_GAME_d_FROM_OBSERVATION_LIST_ 652
ENG(FORMAT_REMOVING_GAME_d_FROM_OBSERVATION_LIST_,"Removing game %d from observation list.")

#define FORMAT_REMPLAYER_FAILED_ 653
ENG(FORMAT_REMPLAYER_FAILED_,"Remplayer failed.")

#define FORMAT_REQUEST_FOR_ADJOURNMENT_SENT_n 654
ENG(FORMAT_REQUEST_FOR_ADJOURNMENT_SENT_n,"Request for adjournment sent.\n")

#define FORMAT_RESET_YOUR_STATS_TO_0 655
ENG(FORMAT_RESET_YOUR_STATS_TO_0,"Reset your stats to 0")

#define FORMAT_SILENT 656
ENG(FORMAT_SILENT,"SILENT")

#define FORMAT_SEE_HTTP_NNGS_COSMIC_ORG_FOR_MORE_INFORMATION_n 657
ENG(FORMAT_SEE_HTTP_NNGS_COSMIC_ORG_FOR_MORE_INFORMATION_n,"See http://nngs.cosmic.org/ for more information.\n")

#define FORMAT_SERVER_UPTIME_u_DAYS_sn 658
ENG(FORMAT_SERVER_UPTIME_u_DAYS_sn,"Server uptime: %lu days, %s\n")

#define FORMAT_SERVER_UPTIME_sn 659
ENG(FORMAT_SERVER_UPTIME_sn,"Server uptime: %s\n")

#define FORMAT_SET_AUTOMAIL_TO_BE_s_ 660
ENG(FORMAT_SET_AUTOMAIL_TO_BE_s_,"Set automail to be %s.")

#define FORMAT_SET_BELL_TO_BE_s_ 661
ENG(FORMAT_SET_BELL_TO_BE_s_,"Set bell to be %s.")

#define FORMAT_SET_CLIENT_TO_BE_s_ 662
ENG(FORMAT_SET_CLIENT_TO_BE_s_,"Set client to be %s.")

#define FORMAT_SET_EXTPROMPT_TO_BE_s_ 663
ENG(FORMAT_SET_EXTPROMPT_TO_BE_s_,"Set extprompt to be %s.")

#define FORMAT_SET_GINFORM_TO_BE_s 664
ENG(FORMAT_SET_GINFORM_TO_BE_s,"Set ginform to be %s")

#define FORMAT_SET_GSHOUT_TO_BE_s_ 665
ENG(FORMAT_SET_GSHOUT_TO_BE_s_,"Set gshout to be %s.")

#define FORMAT_SET_KIBITZ_TO_BE_s_ 666
ENG(FORMAT_SET_KIBITZ_TO_BE_s_,"Set kibitz to be %s.")

#define FORMAT_SET_LOOKING_TO_BE_s_ 667
ENG(FORMAT_SET_LOOKING_TO_BE_s_,"Set looking to be %s.")

#define FORMAT_SET_LSHOUT_TO_BE_s_ 668
ENG(FORMAT_SET_LSHOUT_TO_BE_s_,"Set lshout to be %s.")

#define FORMAT_SET_NOTIFIED_TO_BE_s_ 669
ENG(FORMAT_SET_NOTIFIED_TO_BE_s_,"Set notified to be %s.")

#define FORMAT_SET_OPEN_TO_BE_s_ 670
ENG(FORMAT_SET_OPEN_TO_BE_s_,"Set open to be %s.")

#define FORMAT_SET_PINFORM_TO_BE_s_ 672
ENG(FORMAT_SET_PINFORM_TO_BE_s_,"Set pinform to be %s.")

#define FORMAT_SET_PRIVATE_TO_BE_s_ 673
ENG(FORMAT_SET_PRIVATE_TO_BE_s_,"Set private to be %s.")

#define FORMAT_SET_QUIET_TO_BE_s_ 674
ENG(FORMAT_SET_QUIET_TO_BE_s_,"Set quiet to be %s.")

#define FORMAT_SET_ROBOT_TO_BE_s_ 675
ENG(FORMAT_SET_ROBOT_TO_BE_s_,"Set robot to be %s.")

#define FORMAT_SET_ROPEN_TO_BE_s_ 676
ENG(FORMAT_SET_ROPEN_TO_BE_s_,"Set ropen to be %s.")

#define FORMAT_SET_SHOUT_TO_BE_s_ 677
ENG(FORMAT_SET_SHOUT_TO_BE_s_,"Set shout to be %s.")

#define FORMAT_SET_TELL_TO_BE_s_ 678
ENG(FORMAT_SET_TELL_TO_BE_s_,"Set tell to be %s.")

#define FORMAT_SET_VERBOSE_TO_BE_s_ 679
ENG(FORMAT_SET_VERBOSE_TO_BE_s_,"Set verbose to be %s.")

#define FORMAT_SETTING_YOU_OPEN_FOR_MATCHES_n 680
ENG(FORMAT_SETTING_YOU_OPEN_FOR_MATCHES_n,"Setting you open for matches.\n")

#define FORMAT_SETTING_YOUR_TO_CHANNEL_dn 683
ENG(FORMAT_SETTING_YOUR_TO_CHANNEL_dn,"Setting your ; to channel %d\n")
CHI(FORMAT_SETTING_YOUR_TO_CHANNEL_dn,"�N�z�� \";\"�]�w���W�D %d\n")
GER(FORMAT_SETTING_YOUR_TO_CHANNEL_dn,"Das ; ist auf Kanal %d gesetzt\n")

#define FORMAT_SIT_BACK_AND_WAIT_FOR_THE_MESSAGE_qGAME_HAS_BEEN_PAIREDq_THEN_MOVE_n 684
ENG(FORMAT_SIT_BACK_AND_WAIT_FOR_THE_MESSAGE_qGAME_HAS_BEEN_PAIREDq_THEN_MOVE_n,"Sit back and wait for the message \"Game has been paired\", then move.\n")

#define FORMAT_SOCKET_d_IS_UNUSED_ 685
ENG(FORMAT_SOCKET_d_IS_UNUSED_,"Socket %d is unused!")

#define FORMAT_SOCKET_d_IS_USED_BY_s 686
ENG(FORMAT_SOCKET_d_IS_USED_BY_s,"Socket %d is used by %s")

#define FORMAT_SORRY_THAT_HANDLE_IS_ALREADY_TAKEN_ 687
ENG(FORMAT_SORRY_THAT_HANDLE_IS_ALREADY_TAKEN_,"Sorry that handle is already taken.")

#define FORMAT_SORRY_GAME_d_IS_A_PRIVATE_GAME_ 688
ENG(FORMAT_SORRY_GAME_d_IS_A_PRIVATE_GAME_,"Sorry, game %d is a private game.")

#define FORMAT_SORRY_SHOUTS_ARE_TURNED_OFF_RIGHT_NOW_ 689
ENG(FORMAT_SORRY_SHOUTS_ARE_TURNED_OFF_RIGHT_NOW_,"Sorry, shouts are turned off right now.  :)")

#define FORMAT_SORRY_THIS_CANNOT_BE_A_LADDER_RATED_GAME_ 690
ENG(FORMAT_SORRY_THIS_CANNOT_BE_A_LADDER_RATED_GAME_,"Sorry, this cannot be a ladder rated game.")

#define FORMAT_SORRY_YOUR_QUERY_CONTAINS_INVALID_CHARACTERS_ 691
ENG(FORMAT_SORRY_YOUR_QUERY_CONTAINS_INVALID_CHARACTERS_,"Sorry, your query contains invalid characters.")

#define FORMAT_START_TIME_MUST_BE_0n 692
ENG(FORMAT_START_TIME_MUST_BE_0n,"Start time must be >= 0\n")

#define FORMAT_THAT_NAME_IS_NOT_PERMITTED 693
ENG(FORMAT_THAT_NAME_IS_NOT_PERMITTED,"That name is not permitted")

#define FORMAT_THAT_STRING_IS_TOO_LONG_d_CHARACTERS_MAX_n 694
ENG(FORMAT_THAT_STRING_IS_TOO_LONG_d_CHARACTERS_MAX_n,"That string is too long.  %d characters max.\n")

#define FORMAT_THAT_STRING_IS_TOO_LONG_n 695
ENG(FORMAT_THAT_STRING_IS_TOO_LONG_n,"That string is too long.\n")

#define FORMAT_THE_BOARDS_ARE_NOT_THE_SAME_SIZE_ 696
ENG(FORMAT_THE_BOARDS_ARE_NOT_THE_SAME_SIZE_,"The boards are not the same size.")

#define FORMAT_THE_CURRENTLY_LOADED_GAMES_n 697
ENG(FORMAT_THE_CURRENTLY_LOADED_GAMES_n,"The currently loaded games:\n")

#define FORMAT_THE_FILE_s_WAS_SENT_TO_sn 698
ENG(FORMAT_THE_FILE_s_WAS_SENT_TO_sn,"The file %s was sent to %s\n")

#define FORMAT_THE_GAME_s_s_HAS_BEEN_TOUCHED_ 699
ENG(FORMAT_THE_GAME_s_s_HAS_BEEN_TOUCHED_,"The game %s-%s has been touched.")

#define FORMAT_THE_SERVER_HAS_BEEN_RUNNING_SINCE_s_GMT_n 700
ENG(FORMAT_THE_SERVER_HAS_BEEN_RUNNING_SINCE_s_GMT_n,"The server has been running since %s GMT.\n")

#define FORMAT_THEN_TYPE_qPAIR_q_WHERE_IS_s_S_GAME_NUMBER_n 701
ENG(FORMAT_THEN_TYPE_qPAIR_q_WHERE_IS_s_S_GAME_NUMBER_n,"Then type \"pair #\" where # is %s's game number.\n")

#define FORMAT_THEN_USE_RAISEDEAD_OLDNAME_NEWNAME_ 702
ENG(FORMAT_THEN_USE_RAISEDEAD_OLDNAME_NEWNAME_,"Then use raisedead [oldname] [newname].")

#define FORMAT_THERE_ARE_CURRENTLY_d_GAMES_WITH_A_HIGH_OF_d_SINCE_LAST_RESTART_n 703
ENG(FORMAT_THERE_ARE_CURRENTLY_d_GAMES_WITH_A_HIGH_OF_d_SINCE_LAST_RESTART_n,"There are currently %d games, with a high of %d since last restart.\n")

#define FORMAT_THERE_ARE_CURRENTLY_d_PLAYERS_WITH_A_HIGH_OF_d_SINCE_LAST_RESTART_n 704
ENG(FORMAT_THERE_ARE_CURRENTLY_d_PLAYERS_WITH_A_HIGH_OF_d_SINCE_LAST_RESTART_n,"There are currently %d players, with a high of %d since last restart.\n")

#define FORMAT_THERE_ARE_NO_PLAYERS_LOGGED_IN_THAT_MATCH_YOUR_FLAG_SET_n 705
ENG(FORMAT_THERE_ARE_NO_PLAYERS_LOGGED_IN_THAT_MATCH_YOUR_FLAG_SET_n,"There are no players logged in that match your flag set.\n")

#define FORMAT_THERE_IS_A_SERIOUS_PROBLEM_WITH_YOUR_GAME_RECORD_THIS_IS_SOMETIMESn 706
ENG(FORMAT_THERE_IS_A_SERIOUS_PROBLEM_WITH_YOUR_GAME_RECORD_THIS_IS_SOMETIMESn,"There is a serious problem with your game record.  This is sometimes\n")

#define FORMAT_THERE_IS_NO_PLAYER_BY_THAT_NAME_n 707
ENG(FORMAT_THERE_IS_NO_PLAYER_BY_THAT_NAME_n,"There is no player by that name.\n")

#define FORMAT_THERE_IS_NO_PROBLEM_NUMBER_d 708
ENG(FORMAT_THERE_IS_NO_PROBLEM_NUMBER_d,"There is no problem number %d")

#define FORMAT_THERE_IS_NO_STORED_GAME_s_VS_sn 709
ENG(FORMAT_THERE_IS_NO_STORED_GAME_s_VS_sn,"There is no stored game %s vs. %s\n")

#define FORMAT_THERE_WAS_AN_INTERNAL_ERROR_PLEASE_NOTIFY_AN_ADMIN_n 710
ENG(FORMAT_THERE_WAS_AN_INTERNAL_ERROR_PLEASE_NOTIFY_AN_ADMIN_n,"There was an internal error.  Please notify an admin!\n")

#define FORMAT_THERE_WAS_AN_INTERNAL_ERROR_ 711
ENG(FORMAT_THERE_WAS_AN_INTERNAL_ERROR_,"There was an internal error.")

#define FORMAT_THIS_CAN_BE_A_LADDER19_RATED_GAME_n 712
ENG(FORMAT_THIS_CAN_BE_A_LADDER19_RATED_GAME_n,"This can be a Ladder19 rated game.\n")

#define FORMAT_THIS_CAN_BE_A_LADDER9_RATED_GAME_n 713
ENG(FORMAT_THIS_CAN_BE_A_LADDER9_RATED_GAME_n,"This can be a Ladder9 rated game.\n")

#define FORMAT_THIS_IS_NOW_A_LADDER_RATED_GAME_n 714
ENG(FORMAT_THIS_IS_NOW_A_LADDER_RATED_GAME_n,"This is now a ladder rated game.\n")

#define FORMAT_TO_DO_SO_PLEASE_TYPE_qPAIR_dqn 715
ENG(FORMAT_TO_DO_SO_PLEASE_TYPE_qPAIR_dqn,"To do so, please type \"pair %d\"\n")

#define FORMAT_TRUE 716
ENG(FORMAT_TRUE,"True")

#define FORMAT_TYPE_LADDER_BEFORE_YOUR_FIRST_MOVE_TO_MAKE_IT_LADDER_RATED_n 717
ENG(FORMAT_TYPE_LADDER_BEFORE_YOUR_FIRST_MOVE_TO_MAKE_IT_LADDER_RATED_n,"Type 'ladder' BEFORE your first move to make it ladder rated.\n")

#define FORMAT_TYPE_OR_qNEXTq_TO_SEE_NEXT_PAGE_n 718
ENG(FORMAT_TYPE_OR_qNEXTq_TO_SEE_NEXT_PAGE_n,"Type ` or \"next\" to see next page.\n")
GER(FORMAT_TYPE_OR_qNEXTq_TO_SEE_NEXT_PAGE_n,"Bitte ` oder \"next\" tippen fuer die naechste Seite.\n")

#define FORMAT_TYPE_MATCH_s_W_SIZE_TIME_BYO_TIMEn 719
ENG(FORMAT_TYPE_MATCH_s_W_SIZE_TIME_BYO_TIMEn,"Type: match %s w size time byo-time\n")

#define FORMAT_UNCENSORING_ALL_ 720
ENG(FORMAT_UNCENSORING_ALL_,"Uncensoring all:")
GER(FORMAT_UNCENSORING_ALL_,"Uncensoring alle:")

#define FORMAT_UNCENSORING_ALLn 721
ENG(FORMAT_UNCENSORING_ALLn,"Uncensoring all\n")
GER(FORMAT_UNCENSORING_ALLn,"Uncensoring alle\n")

#define FORMAT_UNDO_IN_GAME_d_s_VS_s_sn 722
ENG(FORMAT_UNDO_IN_GAME_d_s_VS_s_sn,"Undo in game %d: %s vs %s:  %s\n")

#define FORMAT_UNSET_YOUR_RANK_INFO 723
ENG(FORMAT_UNSET_YOUR_RANK_INFO,"Unset your Rank Info")

#define FORMAT_UNSET_YOUR_REAL_NAME_INFO 724
ENG(FORMAT_UNSET_YOUR_REAL_NAME_INFO,"Unset your Real Name Info")

#define FORMAT_UPDATING_KOMI_OFFER_TO_ 725
ENG(FORMAT_UPDATING_KOMI_OFFER_TO_,"Updating komi offer to: ")

#define FORMAT_USAGE_n 726
ENG(FORMAT_USAGE_n,"Usage: \n")
GER(FORMAT_USAGE_n,"Anwendung: \n")

#define FORMAT_USE_HANDICAP_1_9_PLEASE_ 727
ENG(FORMAT_USE_HANDICAP_1_9_PLEASE_,"Use 'handicap 1 - 9' please.")
GER(FORMAT_USE_HANDICAP_1_9_PLEASE_,"Bitte 'handicap 1 - 9' benutzen.")

#define FORMAT_USE_HANDICAP_9_THEN_ASK_WHITE_TO_PASS_PLEASE_ 728
ENG(FORMAT_USE_HANDICAP_9_THEN_ASK_WHITE_TO_PASS_PLEASE_,"Use 'handicap 9' then ask White to pass, please.")

#define FORMAT_USE_GOEMATCH_s_s_d_d_OR_DECLINE_s_TO_RESPOND_n 729
ENG(FORMAT_USE_GOEMATCH_s_s_d_d_OR_DECLINE_s_TO_RESPOND_n,"Use <goematch %s %s %d %d> or <decline %s> to respond.\n")

#define FORMAT_USE_MATCH_s_s_d_d_d_OR_DECLINE_s_TO_RESPOND_n 730
ENG(FORMAT_USE_MATCH_s_s_d_d_d_OR_DECLINE_s_TO_RESPOND_n,"Use <match %s %s %d %d %d> or <decline %s> to respond.\n")
GER(FORMAT_USE_MATCH_s_s_d_d_d_OR_DECLINE_s_TO_RESPOND_n,"Bitte <match %s %s %d %d %d> oder <decline %s> antworten.\n")

#define FORMAT_USE_TMATCH_s_s_d_d_d_OR_DECLINE_s_TO_RESPOND_n 731
ENG(FORMAT_USE_TMATCH_s_s_d_d_d_OR_DECLINE_s_TO_RESPOND_n,"Use <tmatch %s %s %d %d %d> or <decline %s> to respond.\n")

#define FORMAT_USE_qTEACHq_FOR_TEACHING_GAMES_n 732
ENG(FORMAT_USE_qTEACHq_FOR_TEACHING_GAMES_n,"Use \"Teach\" for Teaching games.\n")

#define FORMAT_USE_qKOMI_fq_TO_ACCEPT_OR_qDECLINE_sq_TO_RESPOND_n 733
ENG(FORMAT_USE_qKOMI_fq_TO_ACCEPT_OR_qDECLINE_sq_TO_RESPOND_n,"Use \"komi %.1f\" to accept, or \"decline %s\" to respond.\n")

#define FORMAT_WE_APOLOGIZE_BUT_THE_GAME_IS_LOST_ 734
ENG(FORMAT_WE_APOLOGIZE_BUT_THE_GAME_IS_LOST_,"We apologize, but the game is lost.")

#define FORMAT_WELCOME_TO_THE_ADMIN_CHANNEL_TOPIC_IS_sn 735
ENG(FORMAT_WELCOME_TO_THE_ADMIN_CHANNEL_TOPIC_IS_sn,"Welcome to the Admin Channel.  Topic is: %s\n")

#define FORMAT_WHITE_HAS_ONLY_d_SECONDS_LEFT_n 736
ENG(FORMAT_WHITE_HAS_ONLY_d_SECONDS_LEFT_n,"White has only %d seconds left.\n")

#define FORMAT_WHITE_s_d_dn 737
ENG(FORMAT_WHITE_s_d_dn,"White(%s) : %d:%02d\n")

#define FORMAT_WIDTH_SET_TO_d_n 738
ENG(FORMAT_WIDTH_SET_TO_d_n,"Width set to %d.\n")

#define FORMAT_WINS_dn 739
ENG(FORMAT_WINS_dn,"Wins:         %d\n")

#define FORMAT_YOUR_ALIASESn 740
ENG(FORMAT_YOUR_ALIASESn,"YOUR ALIASES\n")

#define FORMAT_YOU_ARE_ALREADY_CENSORING_s_ 741
ENG(FORMAT_YOU_ARE_ALREADY_CENSORING_s_,"You are already censoring %s.")

#define FORMAT_YOU_ARE_ALREADY_CENSORING_THE_MAXIMUM_NUMBER_OF_PLAYERS_ 742
ENG(FORMAT_YOU_ARE_ALREADY_CENSORING_THE_MAXIMUM_NUMBER_OF_PLAYERS_,"You are already censoring the maximum number of players.")

#define FORMAT_YOU_ARE_ALREADY_OBSERVING_THE_MAXIMUM_NUMBER_OF_GAMES_n 743
ENG(FORMAT_YOU_ARE_ALREADY_OBSERVING_THE_MAXIMUM_NUMBER_OF_GAMES_n,"You are already observing the maximum number of games.\n")

#define FORMAT_YOU_ARE_CENSORING_qsq_n 744
ENG(FORMAT_YOU_ARE_CENSORING_qsq_n,"You are censoring \"%s\".\n")

#define FORMAT_YOU_ARE_NOT_IN_CHANNEL_d_ 745
ENG(FORMAT_YOU_ARE_NOT_IN_CHANNEL_d_,"You are not in channel %d.")

#define FORMAT_YOU_ARE_NOT_PLAYING_A_GAME_n 746
ENG(FORMAT_YOU_ARE_NOT_PLAYING_A_GAME_n,"You are not playing a game.\n")

#define FORMAT_YOU_ARE_NOT_PLAYING_OR_OBSERVING_THAT_GAME_ 747
ENG(FORMAT_YOU_ARE_NOT_PLAYING_OR_OBSERVING_THAT_GAME_,"You are not playing or observing that game.")

#define FORMAT_YOU_ARE_NOW_AT_POSITION_d_IN_THE_19X19_LADDER_CONGRATS_n 748
ENG(FORMAT_YOU_ARE_NOW_AT_POSITION_d_IN_THE_19X19_LADDER_CONGRATS_n,"You are now at position %d in the 19x19 ladder.  Congrats!\n")

#define FORMAT_YOU_ARE_NOW_AT_POSITION_d_IN_THE_19X19_LADDER_n 749
ENG(FORMAT_YOU_ARE_NOW_AT_POSITION_d_IN_THE_19X19_LADDER_n,"You are now at position %d in the 19x19 ladder.\n")

#define FORMAT_YOU_ARE_NOW_AT_POSITION_d_IN_THE_9X9_LADDER_CONGRATS_n 750
ENG(FORMAT_YOU_ARE_NOW_AT_POSITION_d_IN_THE_9X9_LADDER_CONGRATS_n,"You are now at position %d in the 9x9 ladder.  Congrats!\n")

#define FORMAT_YOU_ARE_NOW_AT_POSITION_d_IN_THE_9X9_LADDER_n 751
ENG(FORMAT_YOU_ARE_NOW_AT_POSITION_d_IN_THE_9X9_LADDER_n,"You are now at position %d in the 9x9 ladder.\n")

#define FORMAT_YOU_ARE_NOW_REGISTERED_CONFIRMATION_TOGETHER_WITH_PASSWORD_IS_SENT_TO_YOURn 752
ENG(FORMAT_YOU_ARE_NOW_REGISTERED_CONFIRMATION_TOGETHER_WITH_PASSWORD_IS_SENT_TO_YOURn,"You are now registered! Confirmation together with password is sent to your\n")

#define FORMAT_YOU_ARE_ONE_OF_THE_OTHER_PLAYERS_CANNOT_PAIR_ 753
ENG(FORMAT_YOU_ARE_ONE_OF_THE_OTHER_PLAYERS_CANNOT_PAIR_,"You are one of the other players.  Cannot pair.")

#define FORMAT_YOU_CAN_ONLY_SET_ADMINLEVEL_FOR_PLAYERS_BELOW_YOUR_ADMINLEVEL_ 754
ENG(FORMAT_YOU_CAN_ONLY_SET_ADMINLEVEL_FOR_PLAYERS_BELOW_YOUR_ADMINLEVEL_,"You can only set adminlevel for players below your adminlevel.")

#define FORMAT_YOU_CAN_ONLY_SET_PASSWORD_FOR_PLAYERS_BELOW_YOUR_ADMINLEVEL_ 755
ENG(FORMAT_YOU_CAN_ONLY_SET_PASSWORD_FOR_PLAYERS_BELOW_YOUR_ADMINLEVEL_,"You can only set password for players below your adminlevel.")

#define FORMAT_YOU_CAN_T_ALIAS_THIS_COMMAND_n 756
ENG(FORMAT_YOU_CAN_T_ALIAS_THIS_COMMAND_n,"You can't alias this command.\n")

#define FORMAT_YOU_CAN_T_CENSOR_YOURSELF_ 757
ENG(FORMAT_YOU_CAN_T_CENSOR_YOURSELF_,"You can't censor yourself.")

#define FORMAT_YOU_CAN_T_CHANGE_YOUR_OWN_ADMINLEVEL_ 758
ENG(FORMAT_YOU_CAN_T_CHANGE_YOUR_OWN_ADMINLEVEL_,"You can't change your own adminlevel.")

#define FORMAT_YOU_CAN_T_PAUSE_UNTIMED_GAMES_ 759
ENG(FORMAT_YOU_CAN_T_PAUSE_UNTIMED_GAMES_,"You can't pause untimed games.")

#define FORMAT_YOU_CAN_T_PROMOTE_SOMEONE_TO_OR_ABOVE_YOUR_ADMINLEVEL_ 760
ENG(FORMAT_YOU_CAN_T_PROMOTE_SOMEONE_TO_OR_ABOVE_YOUR_ADMINLEVEL_,"You can't promote someone to or above your adminlevel.")

#define FORMAT_YOU_CAN_T_REMOVE_AN_ADMIN_WITH_A_LEVEL_HIGHER_THAN_OR_EQUAL_TO_YOURSELF_ 761
ENG(FORMAT_YOU_CAN_T_REMOVE_AN_ADMIN_WITH_A_LEVEL_HIGHER_THAN_OR_EQUAL_TO_YOURSELF_,"You can't remove an admin with a level higher than or equal to yourself.")

#define FORMAT_YOU_CAN_T_SET_HANDLES_FOR_AN_ADMIN_WITH_A_LEVEL_HIGHER_THAN_OR_EQUAL_TO_YOURSELF_ 762
ENG(FORMAT_YOU_CAN_T_SET_HANDLES_FOR_AN_ADMIN_WITH_A_LEVEL_HIGHER_THAN_OR_EQUAL_TO_YOURSELF_,"You can't set handles for an admin with a level higher than or equal to yourself.")

#define FORMAT_YOU_CANNOT_LOAD_SOMEONE_ELSE_S_GAME_TRY_qLOOKq_TO_SEE_THE_GAME_ 763
ENG(FORMAT_YOU_CANNOT_LOAD_SOMEONE_ELSE_S_GAME_TRY_qLOOKq_TO_SEE_THE_GAME_,"You cannot load someone else's game.  Try \"look\" to see the game.")

#define FORMAT_YOU_CANNOT_OBSERVE_A_GAME_THAT_YOU_ARE_PLAYING_ 764
ENG(FORMAT_YOU_CANNOT_OBSERVE_A_GAME_THAT_YOU_ARE_PLAYING_,"You cannot observe a game that you are playing.")

#define FORMAT_YOU_CANNOT_REVIEW_MULTIPLE_SGF_FILES_n 765
ENG(FORMAT_YOU_CANNOT_REVIEW_MULTIPLE_SGF_FILES_n,"You cannot review multiple SGF files.\n")

#define FORMAT_YOU_CANNOT_TOUCH_SOMEONE_ELSES_STORED_GAME_n 766
ENG(FORMAT_YOU_CANNOT_TOUCH_SOMEONE_ELSES_STORED_GAME_n,"You cannot touch someone elses stored game.\n")

#define FORMAT_YOU_DECLINE_THE_ADJOURN_REQUEST_FROM_s_n 767
ENG(FORMAT_YOU_DECLINE_THE_ADJOURN_REQUEST_FROM_s_n,"You decline the adjourn request from %s.\n")

#define FORMAT_YOU_DECLINE_THE_MATCH_OFFER_FROM_s_n 768
ENG(FORMAT_YOU_DECLINE_THE_MATCH_OFFER_FROM_s_n,"You decline the match offer from %s.\n")

#define FORMAT_YOU_DECLINE_THE_PAIR_REQUEST_FROM_s_n 769
ENG(FORMAT_YOU_DECLINE_THE_PAIR_REQUEST_FROM_s_n,"You decline the pair request from %s.\n")

#define FORMAT_YOU_DECLINE_THE_PAUSE_REQUEST_FROM_s_n 770
ENG(FORMAT_YOU_DECLINE_THE_PAUSE_REQUEST_FROM_s_n,"You decline the pause request from %s.\n")

#define FORMAT_YOU_DECLINE_THE_TEACHING_MATCH_OFFER_FROM_s_n 771
ENG(FORMAT_YOU_DECLINE_THE_TEACHING_MATCH_OFFER_FROM_s_n,"You decline the teaching match offer from %s.\n")

#define FORMAT_YOU_HAVE_CENSORED_ 772
ENG(FORMAT_YOU_HAVE_CENSORED_,"You have censored:")

#define FORMAT_YOU_HAVE_MORE_THAN_ONE_PENDING_OFFER_n 773
ENG(FORMAT_YOU_HAVE_MORE_THAN_ONE_PENDING_OFFER_n,"You have more than one pending offer.\n")

#define FORMAT_YOU_HAVE_NO_ALIAS_NAMED_s_ 774
ENG(FORMAT_YOU_HAVE_NO_ALIAS_NAMED_s_,"You have no alias named '%s'.")

#define FORMAT_YOU_HAVE_NO_ALIAS_NAMED_s_n 775
ENG(FORMAT_YOU_HAVE_NO_ALIAS_NAMED_s_n,"You have no alias named '%s'.\n")

#define FORMAT_YOU_HAVE_NO_ONE_CENSORED_ 776
ENG(FORMAT_YOU_HAVE_NO_ONE_CENSORED_,"You have no one censored.")

#define FORMAT_YOU_HAVE_ONLY_d_SECONDS_LEFT_n 777
ENG(FORMAT_YOU_HAVE_ONLY_d_SECONDS_LEFT_n,"You have only %d seconds left.\n")

#define FORMAT_YOU_HAVE_YOUR_MAXIMUM_OF_d_ALIASES_n 778
ENG(FORMAT_YOU_HAVE_YOUR_MAXIMUM_OF_d_ALIASES_n,"You have your maximum of %d aliases.\n")

#define FORMAT_YOU_MUST_BE_ONE_OF_THE_FOUR_PLAYERS_LISTED_IN_THE_SPAIR_COMMAND 779
ENG(FORMAT_YOU_MUST_BE_ONE_OF_THE_FOUR_PLAYERS_LISTED_IN_THE_SPAIR_COMMAND,"You must be one of the four players listed in the spair command")

#define FORMAT_YOU_MUST_FIRST_JOIN_THE_LADDER_WITH_qJOINq_ 780
ENG(FORMAT_YOU_MUST_FIRST_JOIN_THE_LADDER_WITH_qJOINq_,"You must first join the ladder with \"join\".")

#define FORMAT_YOU_MUST_LIST_4_UNIQUE_PLAYER_NAMES_TO_PLAY_PAIR_GO_ 781
ENG(FORMAT_YOU_MUST_LIST_4_UNIQUE_PLAYER_NAMES_TO_PLAY_PAIR_GO_,"You must list 4 unique player names to play pair go.")

#define FORMAT_YOU_MUST_PAIR_BEFORE_YOUR_FIRST_MOVE_OR_AT_EQUAL_MOVES_IN_A_MATCH 782
ENG(FORMAT_YOU_MUST_PAIR_BEFORE_YOUR_FIRST_MOVE_OR_AT_EQUAL_MOVES_IN_A_MATCH,"You must pair before your first move, or at equal moves in a match")

#define FORMAT_YOU_NEED_REVIEW_A_GAME_REVIEW_LOADED_GAME_FILE_IS_A_LOADED_FILE_ 783
ENG(FORMAT_YOU_NEED_REVIEW_A_GAME_REVIEW_LOADED_GAME_FILE_IS_A_LOADED_FILE_,"You need review a game. 'review <loaded game>'.  File is a loaded file.")

#define FORMAT_YOU_WILL_NEED_TO_TYPE_qDONEq_AGAIN_n 784
ENG(FORMAT_YOU_WILL_NEED_TO_TYPE_qDONEq_AGAIN_n,"You will need to type \"done\" again.\n")

#define FORMAT_YOU_WITHDRAW_THE_ADJOURN_REQUEST_TO_s_n 785
ENG(FORMAT_YOU_WITHDRAW_THE_ADJOURN_REQUEST_TO_s_n,"You withdraw the adjourn request to %s.\n")

#define FORMAT_YOU_WITHDRAW_THE_PAIR_REQUEST_TO_s_n 786
ENG(FORMAT_YOU_WITHDRAW_THE_PAIR_REQUEST_TO_s_n,"You withdraw the pair request to %s.\n")

#define FORMAT_YOU_WITHDRAW_THE_PAUSE_REQUEST_TO_s_n 787
ENG(FORMAT_YOU_WITHDRAW_THE_PAUSE_REQUEST_TO_s_n,"You withdraw the pause request to %s.\n")

#define FORMAT_YOU_WITHDRAW_THE_TEACHING_MATCH_OFFER_TO_s_n 788
ENG(FORMAT_YOU_WITHDRAW_THE_TEACHING_MATCH_OFFER_TO_s_n,"You withdraw the teaching match offer to %s.\n")

#define FORMAT_YOU_AND_s_ARE_THE_BLACK_TEAM_n 789
ENG(FORMAT_YOU_AND_s_ARE_THE_BLACK_TEAM_n,"You, and %s are the Black team.\n")

#define FORMAT_YOUR_IS_s_ 790
ENG(FORMAT_YOUR_IS_s_,"Your . is %s.")
GER(FORMAT_YOUR_IS_s_,"Ihr . ist %s.")

#define FORMAT_YOUR_IS_CHANNEL_d_ 791
ENG(FORMAT_YOUR_IS_CHANNEL_d_,"Your ; is channel %d.")
GER(FORMAT_YOUR_IS_CHANNEL_d_,"Ihr ; ist Kanal %d.")

#define FORMAT_YOUR_BUSY_STRING_IS_SET_TO_qsqn 792
ENG(FORMAT_YOUR_BUSY_STRING_IS_SET_TO_qsqn,"Your busy string is set to \"%s\"\n")

#define FORMAT_YOUR_BUSY_STRING_WAS_CLEARED_n 793
ENG(FORMAT_YOUR_BUSY_STRING_WAS_CLEARED_n,"Your busy string was cleared.\n")

#define FORMAT_YOUR_GAME_IS_PAIRED_ALREADY_ 794
ENG(FORMAT_YOUR_GAME_IS_PAIRED_ALREADY_,"Your game is paired already.")

#define FORMAT_YOUR_MOVE_IS_NOT_VALID_n 795
ENG(FORMAT_YOUR_MOVE_IS_NOT_VALID_n,"Your move is not valid.\n")
GER(FORMAT_YOUR_MOVE_IS_NOT_VALID_n,"Ihr Spielzug ist ungueltig.\n")

#define FORMAT_YOUR_NOTE_SAVED_n 796
ENG(FORMAT_YOUR_NOTE_SAVED_n,"Your note saved.\n")
CHI(FORMAT_YOUR_NOTE_SAVED_n,"�z���ܤw�����b�d���O�W.\n")
GER(FORMAT_YOUR_NOTE_SAVED_n,"Ihre Notiz ist abgespeichert.\n")

#define FORMAT_YOUR_NOTIFY_LIST_IS_EMPTY_n 797
ENG(FORMAT_YOUR_NOTIFY_LIST_IS_EMPTY_n,"Your notify list is empty.\n")
GER(FORMAT_YOUR_NOTIFY_LIST_IS_EMPTY_n,"Ihre notify Liste ist leer.\n")

#define FORMAT_YOUR_OPPONENT_s_WILL_FIRST_REQUEST_A_MATCH_WITH_YOU_n 798
ENG(FORMAT_YOUR_OPPONENT_s_WILL_FIRST_REQUEST_A_MATCH_WITH_YOU_n,"Your opponent %s will first request a match with you.\n")

#define FORMAT_YOUR_OPPONENT_IS_NOT_OPEN_FOR_MATCHES_n 799
ENG(FORMAT_YOUR_OPPONENT_IS_NOT_OPEN_FOR_MATCHES_n,"Your opponent is not open for matches.\n")
GER(FORMAT_YOUR_OPPONENT_IS_NOT_OPEN_FOR_MATCHES_n,"Ihr Gegner akzeptiert keine Matches.\n")

#define FORMAT_007N 800
ENG(FORMAT_007N,"\007\n")

#define FORMAT_Gn 801
ENG(FORMAT_Gn,"^G\n")

#define FORMAT_WARNING_YOUR_DATA_FILE_IS_CORRUPT_PLEASE_TELL_AN_ADMIN_n 802
ENG(FORMAT_WARNING_YOUR_DATA_FILE_IS_CORRUPT_PLEASE_TELL_AN_ADMIN_n,"*** WARNING: Your Data file is corrupt. Please tell an admin ***\n")

#define FORMAT_nFOUND_d_ADMINs_TO_HELP_YOU_ 803
ENG(FORMAT_nFOUND_d_ADMINs_TO_HELP_YOU_,"\nFound %d admin%s to help you.")

#define FORMAT_SEE_HELP_s_FOR_A_COMPLETE_DESCRIPTION_n 804
ENG(FORMAT_SEE_HELP_s_FOR_A_COMPLETE_DESCRIPTION_n,"See 'help %s' for a complete description.\n")
GER(FORMAT_SEE_HELP_s_FOR_A_COMPLETE_DESCRIPTION_n,"Bitte 'help %s' fuer eine komplette Beschreibung konsultieren.\n")

#define FORMAT_s_s_n 805
ENG(FORMAT_s_s_n,"^ %s %s ^\n")

#define FORMAT_s_ 806
ENG(FORMAT_s_,"^ %s ^")

#define FORMAT_ADDTIME_HAS_A_MAXIMUM_LIMIT_OF_60000_MINUTES_ 807
ENG(FORMAT_ADDTIME_HAS_A_MAXIMUM_LIMIT_OF_60000_MINUTES_,"addtime has a maximum limit of 60000 minutes.")
GER(FORMAT_ADDTIME_HAS_A_MAXIMUM_LIMIT_OF_60000_MINUTES_,"addtime kann maximal 60000 minuten addieren.")

#define FORMAT_ADDTIME_REQUIRES_AN_INTEGER_VALUE_GREATER_THAN_ZERO_ 808
ENG(FORMAT_ADDTIME_REQUIRES_AN_INTEGER_VALUE_GREATER_THAN_ZERO_,"addtime requires an integer value greater than zero.")
GER(FORMAT_ADDTIME_REQUIRES_AN_INTEGER_VALUE_GREATER_THAN_ZERO_,"addtime braucht eine Zahl groesser als null.")

#define FORMAT_ADMINLEVEL_dn 809
ENG(FORMAT_ADMINLEVEL_dn,"adminLevel = %d\n")

#define FORMAT_BMUZZLED_dn 810
ENG(FORMAT_BMUZZLED_dn,"bmuzzled = %d\n")

#define FORMAT_CAUSED_BY_AN_NNGS_CRASH_DURING_YOUR_GAME_ 811
ENG(FORMAT_CAUSED_BY_AN_NNGS_CRASH_DURING_YOUR_GAME_,"caused by an NNGS crash during your game.  ")

#define FORMAT_EMAILADDRESS_sn 812
ENG(FORMAT_EMAILADDRESS_sn,"email = %s\n")

#define FORMAT_FULLNAME_sn 813
ENG(FORMAT_FULLNAME_sn,"fullname = %s\n")

#define FORMAT_GMUZZLED_dn 814
ENG(FORMAT_GMUZZLED_dn,"gmuzzled = %d\n")

#define FORMAT_LASTHOST_s 815
ENG(FORMAT_LASTHOST_s,"lastHost = %s")

#define FORMAT_LAST_CHANNEL_dn 816
ENG(FORMAT_LAST_CHANNEL_dn,"last_channel = %d\n")

#define FORMAT_LAST_TELL_dn 817
ENG(FORMAT_LAST_TELL_dn,"last_tell = %d\n")

#define FORMAT_LOGIN_sn 818
ENG(FORMAT_LOGIN_sn,"login = %s\n")

#define FORMAT_LOGON_TIME_dn 819
ENG(FORMAT_LOGON_TIME_dn,"logon_time = %d\n")

#define FORMAT_MUZZLED_dn 820
ENG(FORMAT_MUZZLED_dn,"muzzled = %d\n")

#define FORMAT_NAME_sn 821
ENG(FORMAT_NAME_sn,"name = %s\n")

#define FORMAT_REGISTERED_dn 822
ENG(FORMAT_REGISTERED_dn,"registered = %d\n")

#define FORMAT_SOCKET_dn 823
ENG(FORMAT_SOCKET_dn,"socket = %d\n")

#define FORMAT_STATE_dn 824
ENG(FORMAT_STATE_dn,"state = %d\n")

#define FORMAT_THISHOST_sn 825
ENG(FORMAT_THISHOST_sn,"thisHost = %s\n")

#define FORMAT_WATER_LEVEL_OF_s_SET_TO_d_ 826
ENG(FORMAT_WATER_LEVEL_OF_s_SET_TO_d_,"water level of %s set to %d.")

#define FORMAT_s_ss_ss_s_HAS_CONNECTED_n 827
ENG(FORMAT_s_ss_ss_s_HAS_CONNECTED_n,"{%s [%3.3s%s] (%s%s: %s) has connected.}\n")
GER(FORMAT_s_ss_ss_s_HAS_CONNECTED_n,"{%s [%3.3s%s] (%s%s: %s) hat den Server betreten.}\n")

#define FORMAT_s_HAS_DISCONNECTED_n 828
ENG(FORMAT_s_HAS_DISCONNECTED_n,"{%s has disconnected}\n")
GER(FORMAT_s_HAS_DISCONNECTED_n,"{%s hat den Server verlassen}\n")

#define FORMAT_GAME_d_s_VS_s_MOVE_d_n 829
ENG(FORMAT_GAME_d_s_VS_s_MOVE_d_n,"{Game %d: %s vs %s @ Move %d}\n")
GER(FORMAT_GAME_d_s_VS_s_MOVE_d_n,"{Spiel %d: %s gegen %s @ Zug %d}\n")

#define FORMAT_s_WOULD_LIKE_TO_ADJOURN_THE_GAME_n 830
ENG(FORMAT_s_WOULD_LIKE_TO_ADJOURN_THE_GAME_n,"%s would like to adjourn the game.\n")
GER(FORMAT_s_WOULD_LIKE_TO_ADJOURN_THE_GAME_n,"%s moechte das Spiel unterbrechen.\n")

#define FORMAT_USE_ADJOURN_TO_ADJOURN_THE_GAME_n 831
ENG(FORMAT_USE_ADJOURN_TO_ADJOURN_THE_GAME_n,"Use adjourn to adjourn the game.\n")
GER(FORMAT_USE_ADJOURN_TO_ADJOURN_THE_GAME_n,"Bitte 'adjourn' benutzen um ein Spiel zu unterbrechen.\n")

#define FORMAT_YOU_ARE_NOT_PLAYING_OR_OBSERVING_A_GAME 832
ENG(FORMAT_YOU_ARE_NOT_PLAYING_OR_OBSERVING_A_GAME,"You are not playing or observing a game")
GER(FORMAT_YOU_ARE_NOT_PLAYING_OR_OBSERVING_A_GAME,"Sie spielen oder observieren kein Spiel")

#define FORMAT_d_MINUTES_WERE_ADDED_TO_YOUR_OPPONENTS_CLOCKn 833
ENG(FORMAT_d_MINUTES_WERE_ADDED_TO_YOUR_OPPONENTS_CLOCKn,"%d minutes were added to your opponents clock\n")
GER(FORMAT_d_MINUTES_WERE_ADDED_TO_YOUR_OPPONENTS_CLOCKn,"%d Minuten auf die Uhr ihres Gegners gesetzt\n")

#define FORMAT_YOUR_OPPONENT_HAS_ADDED_d_MINUTES_TO_YOUR_CLOCK_n 834
ENG(FORMAT_YOUR_OPPONENT_HAS_ADDED_d_MINUTES_TO_YOUR_CLOCK_n,"your opponent has added %d minutes to your clock.\n")
GER(FORMAT_YOUR_OPPONENT_HAS_ADDED_d_MINUTES_TO_YOUR_CLOCK_n,"Ihr Gegner hat %d Minuten auf ihre Uhr gesetzt.\n")

#if 0 /* AvK_OUDE_MEUK */
#define FORMAT_THE_PLAYER_s_IS_NOW_IN_BYO_YOMI_n 835
ENG(FORMAT_THE_PLAYER_s_IS_NOW_IN_BYO_YOMI_n,"The player %s is now in byo yomi.\n")
GER(FORMAT_THE_PLAYER_s_IS_NOW_IN_BYO_YOMI_n,"Der Spieler %s ist jetzt in byo yomi.\n")


#define FORMAT_sTHE_PLAYER_s_IS_NOW_IN_BYO_YOMI_n 836
ENG(FORMAT_sTHE_PLAYER_s_IS_NOW_IN_BYO_YOMI_n,"%sThe player %s is now in byo yomi.\n")
GER(FORMAT_sTHE_PLAYER_s_IS_NOW_IN_BYO_YOMI_n,"%sDer Spieler %s ist jetzt in byo yomi.\n")
#endif

#define FORMAT_HE_HAS_d_STONES_AND_d_MINUTESn 835
ENG(FORMAT_HE_HAS_d_STONES_AND_d_MINUTESn,"He has %d stones and %d minutes.\n")

#define FORMAT_SERVER_GOING_DOWN_IN_d_MINUTES_AND_d_SECONDS_n 837
ENG(FORMAT_SERVER_GOING_DOWN_IN_d_MINUTES_AND_d_SECONDS_n,"Server going down in %d minutes and %d seconds.\n")

#define FORMAT_LANGUAGE_SET_TO_d_n 838
ENG(FORMAT_LANGUAGE_SET_TO_d_n,"Language set to %d.\n")
GER(FORMAT_LANGUAGE_SET_TO_d_n,"Sprache zu %d gesetzt.\n")

#define FORMAT_s_s_s_s_s_s_s_s 839
ENG(FORMAT_s_s_s_s_s_s_s_s,"%s %s   %s %-10s %3s    %3.3s%s %s")

#define FORMAT_s_ss_HAS_CONNECTED_n 840
ENG(FORMAT_s_ss_HAS_CONNECTED_n,"{%s [%3.3s%s] has connected.}\n")
GER(FORMAT_s_ss_HAS_CONNECTED_n,"{%s [%3.3s%s] hat den Server betreten.}\n")

#define FORMAT_SERVER_NAME_sn 841
ENG(FORMAT_SERVER_NAME_sn,"Server name: %s\n")

#define FORMAT_SERVER_ADDRESS_sn 842
ENG(FORMAT_SERVER_ADDRESS_sn,"Server address: %s\n")

#define FORMAT_SERVER_VERSION_sn 843
ENG(FORMAT_SERVER_VERSION_sn,"Server version: %s\n")

#define FORMAT_NEED_IP_ADDRESSn 844
ENG(FORMAT_NEED_IP_ADDRESSn,"Need an ip-address\n")

#define FORMAT_NEED_NAMEn 845
ENG(FORMAT_NEED_NAMEn,"Need a name\n")

#define FORMAT_IP_ADDRESS_s_s_BAN_IS_dn 846
ENG(FORMAT_IP_ADDRESS_s_s_BAN_IS_dn,"IP-address %s-%s: ban is %d\n")
 
#define FORMAT_PLAYER_s_AT_s_sBANNED_RANGE_sONLINEn 847
ENG(FORMAT_PLAYER_s_AT_s_sBANNED_RANGE_sONLINEn,"Player %s at %s (%sbanned range) %sonline\n")

#define FORMAT_NEED_ADCP_IP_ADDRESSn 848
ENG(FORMAT_NEED_ADCP_IP_ADDRESSn,"Need {adcp+-?.} IP-address [IP-address]\n")

#define FORMAT_IP_BAN_CHANGED_dn 849
ENG(FORMAT_IP_BAN_CHANGED_dn,"IP-address-bans changed: %d\n")

#define FORMAT_IP_RANGES_n 850
ENG(FORMAT_IP_RANGES_n,"IP-address-ban ranges:\n")

#define FORMAT__s 851
ENG(FORMAT__s," %s")

/** end insert here */
#if WANT_TO_DEFINE
  } 
#endif /* WANT_TO_DEFINE */

#endif /* FORMATS_H */
