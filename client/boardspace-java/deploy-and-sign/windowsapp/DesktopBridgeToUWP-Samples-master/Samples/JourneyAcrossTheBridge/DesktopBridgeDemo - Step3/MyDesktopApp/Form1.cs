﻿using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using Microsoft.Win32;
using Windows.UI.Notifications;
using Windows.Data.Xml.Dom;
using Windows.Storage;

namespace MyDesktopApp
{
    public partial class Form1 : Form
    {
        bool initialized = false; 
        public Form1()
        {
            InitializeComponent();        
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            foreach (var item in comboBox1.Items)
            {
                if (item.ToString() == Program.CurrentStatus)
                {
                    comboBox1.SelectedItem = item;
                    if (ApplicationData.Current.LocalSettings.Values.ContainsKey("Status"))
                    {
                        ApplicationData.Current.LocalSettings.Values["Status"] = Program.CurrentStatus;
                    }
                    else
                    {
                        ApplicationData.Current.LocalSettings.Values.Add("Status", Program.CurrentStatus);
                    }
                    initialized = true;
                    break;
                }
            }
        }

        private void comboBox1_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (!initialized) return;
            Program.RegKey.SetValue("Status", comboBox1.SelectedItem);
            ApplicationData.Current.LocalSettings.Values["Status"] = comboBox1.SelectedItem.ToString();

            // Update the apps live tile
            XmlDocument tileXml = TileUpdateManager.GetTemplateContent(TileTemplateType.TileSquare150x150Text03);

            XmlNodeList textNodes = tileXml.GetElementsByTagName("text");
            textNodes[0].InnerText = "MyDesktopApp";
            textNodes[1].InnerText = "Status: ";
            textNodes[2].InnerText = comboBox1.SelectedItem.ToString();
            textNodes[3].InnerText = DateTime.Now.ToString("HH:mm:ss");

            TileNotification tileNotification = new TileNotification(tileXml);
            TileUpdateManager.CreateTileUpdaterForApplication().Update(tileNotification);
        }
    }
}
