﻿//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// This code is licensed under the MIT License (MIT).
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

using System;
using Windows.Foundation.Collections;
using Windows.UI.Xaml.Controls;
using Windows.UI.Notifications;
using Windows.Data.Xml.Dom;
using Windows.UI.Xaml.Navigation;
using Windows.UI.Popups;
using Windows.ApplicationModel;
using Windows.ApplicationModel.AppService;
using Windows.ApplicationModel.Background;

// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x409

namespace MyUWPApp
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        /// <summary>
        /// Intialize the view
        /// </summary>
        public MainPage()
        {
            this.InitializeComponent();
            (App.Current as App).StatusUpdated += MainPage_StatusUpdated;
        }

        /// <summary>
        /// Create animation and register for a time trigger
        /// </summary>
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);
            colorAnimation.Begin();
            LaunchBackgroundProcess();
            RegisterBackgroundTask("TimerTrigger", new TimeTrigger(15, false));
        }

        /// <summary>
        /// Registers the background task
        /// </summary>
        private void RegisterBackgroundTask(String triggerName, IBackgroundTrigger trigger)
        {
            // Check if the task is already registered
            foreach (var cur in BackgroundTaskRegistration.AllTasks)
            {
                if (cur.Value.Name == triggerName)
                {
                    // The task is already registered.
                    return;
                }
            }

            BackgroundTaskBuilder builder = new BackgroundTaskBuilder();
            builder.Name = triggerName;
            builder.SetTrigger(trigger);
            builder.TaskEntryPoint = "BackgroundTasks.MyBackgroundTask";
            builder.Register();
        }

        /// <summary>
        /// Updates the comboBox with the selected item
        /// </summary>
        private async void MainPage_StatusUpdated(object sender, string e)
        {
            await Dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
            {
                foreach (ComboBoxItem item in comboBox1.Items)
                {
                    if (item.Content.ToString() == e)
                    {
                        comboBox1.SelectedItem = item;
                        break;
                    }
                }
            });            
        }

        private async void LaunchBackgroundProcess()
        {
            try
            {
                // Make sure MyDesktopApp.exe is in your AppX folder, if not rebuild the solution
                await FullTrustProcessLauncher.LaunchFullTrustProcessForCurrentAppAsync();
            }
            catch (Exception)
            {
                MessageDialog dialog = new MessageDialog("Rebuild the solution and make sure the BackgroundProcess is in your AppX folder");
                await dialog.ShowAsync();
            }
        }

        /// <summary>
        /// Updates the tile when the state changed
        /// </summary>
        private async void comboBox1_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            string value = (comboBox1.SelectedItem as ComboBoxItem).Content.ToString();
            ValueSet valueSet = new ValueSet();
            valueSet.Add("StatusUpdate", value);

            if (App.Connection != null)
            {
                AppServiceResponse response = await App.Connection.SendMessageAsync(valueSet);
            }            

            // Update the apps live tile
            XmlDocument tileXml = TileUpdateManager.GetTemplateContent(TileTemplateType.TileSquare150x150Text03);

            XmlNodeList textNodes = tileXml.GetElementsByTagName("text");
            textNodes[0].InnerText = "MyDesktopApp";
            textNodes[1].InnerText = "Status: ";
            textNodes[2].InnerText = value;
            textNodes[3].InnerText = DateTime.Now.ToString("HH:mm:ss");

            TileNotification tileNotification = new TileNotification(tileXml);
            TileUpdateManager.CreateTileUpdaterForApplication().Update(tileNotification);
        }
    }
}
